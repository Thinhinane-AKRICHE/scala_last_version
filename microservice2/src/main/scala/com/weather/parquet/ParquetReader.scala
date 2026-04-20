package com.weather.parquet

import org.apache.spark.sql.SparkSession

object ParquetReader {

  def readParquet(parquetPath: String): Unit = {
    val spark = SparkSession.builder()
      .appName("WeatherMicroservice2")
      .master("local[*]")
      .getOrCreate()

    val df = spark.read.parquet(parquetPath)

    println(s"\n[SPARK] Nombre de records : ${df.count()}")
    println(s"[SPARK] Schema :")
    df.printSchema()
    println(s"[SPARK] Aperçu :")
    df.select("fileName", "label", "labelId", "width", "height").show(5)
    println(s"[SPARK] Distribution par label :")
    df.groupBy("label").count().orderBy("label").show()

    spark.stop()
  }
}