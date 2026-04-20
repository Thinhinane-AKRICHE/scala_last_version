package com.weather

import com.weather.domain.TempImageRecord
import com.weather.transform.{ImagePreprocessor, LabelEncoder}
import org.apache.spark.sql.{Encoders, SaveMode, SparkSession}

import scala.util.Try

object Main {

  def main(args: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "C:/hadoop")

    val rawDir = "C:/spark-data/dataset"
    val outputParquet =
      "C:/Users/akric/Downloads/weather-project/weather-project/Weather-Image-Recognition/weather.parquet"

    val spark = SparkSession.builder()
      .appName("WeatherMicroservice1")
      .master("local[*]")
      .config("spark.driver.memory", "4g")
      .getOrCreate()

    import spark.implicits._

    println("=== START PARSER ===")

    val binaryDf = spark.read
      .format("binaryFile")
      .option("pathGlobFilter", "*.jpg")
      .option("recursiveFileLookup", "true")
      .load(rawDir)
      .select("path", "content")

    val imageCount = binaryDf.count()
    println(s"[INFO] $imageCount images trouvées")

    if (imageCount == 0) {
      println("[ERROR] Aucun fichier trouvé")
      spark.stop()
      return
    }

    val labelPaths = binaryDf.select("path").as[String].collect().toSeq
    val labels = labelPaths.map(extractLabelFromPath)
    val labelMap = LabelEncoder.buildLabelMap(labels)

    println(s"[INFO] Labels: $labelMap")

    val bcLabelMap = spark.sparkContext.broadcast(labelMap)

    val recordsDS = binaryDf.as[(String, Array[Byte])](
      Encoders.tuple(Encoders.STRING, Encoders.BINARY)
    ).map { case (path, content) =>
      Try {
        val fileName = path.split("[/\\\\]").last
        val label = extractLabelFromPath(path)
        val labelId = bcLabelMap.value(label)

        val (width, height, channels, features) =
          ImagePreprocessor.preprocessBytes(content, 64, 64)

        Some(
          TempImageRecord(
            imagePath = path,
            fileName = fileName,
            label = label,
            labelId = labelId,
            width = width,
            height = height,
            channels = channels,
            features = features
          )
        )
      }.getOrElse(None)
    }(Encoders.kryo[Option[TempImageRecord]])
      .filter(_.isDefined)
      .map(_.get)(Encoders.kryo[TempImageRecord])

    val validCount = recordsDS.count()
    println(s"[INFO] $validCount records valides")

    if (validCount == 0) {
      println("[ERROR] Aucun record à écrire")
      spark.stop()
      return
    }

    val finalDf = recordsDS.toDF()

    finalDf.write
      .mode(SaveMode.Overwrite)
      .parquet(outputParquet)

    println(s"[SUCCESS] Parquet généré : $outputParquet")
    println("=== END PARSER ===")

    spark.stop()
  }

  private def extractLabelFromPath(path: String): String = {
    val normalized = path.replace("\\", "/")
    normalized.split("/").dropRight(1).last.toLowerCase
  }
}