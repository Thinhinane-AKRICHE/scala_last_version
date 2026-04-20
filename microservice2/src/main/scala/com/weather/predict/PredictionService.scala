package com.weather.predict

import java.net.{HttpURLConnection, URL}
import java.io.{File, DataOutputStream}
import java.nio.file.Files
import scala.io.Source

object PredictionService {

  private val apiUrl = "http://localhost:8000/predict"

  def predict(imageFile: File): (String, Double, String) = {
    val boundary = "----WeatherBoundary"
    val url = new URL(apiUrl)
    val conn = url.openConnection().asInstanceOf[HttpURLConnection]

    try {
      conn.setRequestMethod("POST")
      conn.setDoOutput(true)
      conn.setRequestProperty("Content-Type", s"multipart/form-data; boundary=$boundary")

      val fileBytes = Files.readAllBytes(imageFile.toPath)
      val os = new DataOutputStream(conn.getOutputStream)

      os.writeBytes(s"--$boundary\r\n")
      os.writeBytes(s"""Content-Disposition: form-data; name="file"; filename="${imageFile.getName}"\r\n""")
      os.writeBytes("Content-Type: image/jpeg\r\n\r\n")
      os.write(fileBytes)
      os.writeBytes(s"\r\n--$boundary--\r\n")
      os.flush()
      os.close()

      val responseCode = conn.getResponseCode
      if (responseCode == 200) {
        val response = Source.fromInputStream(conn.getInputStream).mkString
        parseResponse(response)
      } else {
        throw new RuntimeException(s"API erreur: code $responseCode")
      }
    } finally {
      conn.disconnect()
    }
  }

  private def parseResponse(json: String): (String, Double, String) = {
    val labelPattern = """"label"\s*:\s*"([^"]+)"""".r
    val confidencePattern = """"confidence"\s*:\s*([\d.]+)""".r
    val predictionsPattern = """"predictions"\s*:\s*(\{[^}]+\})""".r

    val label = labelPattern.findFirstMatchIn(json).map(_.group(1)).getOrElse("unknown")
    val confidence = confidencePattern.findFirstMatchIn(json).map(_.group(1).toDouble).getOrElse(0.0)
    val predictions = predictionsPattern.findFirstMatchIn(json).map(_.group(1)).getOrElse("{}")

    (label, confidence, predictions)
  }
}