package com.weather

import com.sun.net.httpserver.{HttpServer, HttpExchange}
import java.net.InetSocketAddress
import java.io.{File, FileOutputStream}
import com.weather.transform.ImagePreprocessor
import com.weather.predict.PredictionService
import com.weather.db.DatabaseManager

object PredictionServer {

  def start(): Unit = {
    val server = HttpServer.create(new InetSocketAddress(9001), 0)
    println("[SERVER] Microservice 2 démarré sur le port 9001")

    // Route /predict
    server.createContext("/predict", (exchange: HttpExchange) => {
      if (exchange.getRequestMethod == "POST") {
        try {
          // 1. Recevoir l'image
          val inputStream = exchange.getRequestBody
          val imageBytes = inputStream.readAllBytes()
          inputStream.close()

          // 2. Sauvegarder temporairement
          val tempOriginal = new File("temp_upload.jpg")
          val fos = new FileOutputStream(tempOriginal)
          fos.write(imageBytes)
          fos.close()

          // 3. Appeler le Microservice 1 (resizeImage)
          val resizedImage = new File("temp_resized.jpg")
          ImagePreprocessor.resizeImage(tempOriginal, resizedImage)
          println(s"[MS1] Image resizée : 224x224")

          // 4. Appeler l'API Python pour la prédiction
          val (label, confidence, allPredictions) =
            PredictionService.predict(resizedImage)
          println(s"[API] Résultat : $label ($confidence%)")

          // 5. Sauvegarder en BDD
          DatabaseManager.savePrediction(
            imageName = tempOriginal.getName,
            predictedLabel = label,
            confidence = confidence,
            allPredictions = allPredictions,
            modelName = "ResNet50",
            modelAccuracy = 98.0
          )

          // 6. Retourner le résultat en JSON
          val json = s"""{"label":"$label","confidence":$confidence,"predictions":$allPredictions}"""
          exchange.getResponseHeaders.set("Content-Type", "application/json")
          exchange.sendResponseHeaders(200, json.getBytes.length)
          val os = exchange.getResponseBody
          os.write(json.getBytes)
          os.close()

          // Nettoyer
          tempOriginal.delete()
          resizedImage.delete()

        } catch {
          case e: Exception =>
            val error = s"""{"error":"${e.getMessage}"}"""
            exchange.sendResponseHeaders(500, error.getBytes.length)
            val os = exchange.getResponseBody
            os.write(error.getBytes)
            os.close()
        }
      }
    })

    // Route /health
    server.createContext("/health", (exchange: HttpExchange) => {
      val json = """{"status":"ok","service":"microservice2"}"""
      exchange.sendResponseHeaders(200, json.getBytes.length)
      val os = exchange.getResponseBody
      os.write(json.getBytes)
      os.close()
    })

    server.setExecutor(null)
    server.start()
  }
}