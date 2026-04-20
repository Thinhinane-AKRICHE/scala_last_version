package com.weather

import com.weather.db.DatabaseManager
import com.weather.predict.PredictionService
import com.weather.parquet.ParquetReader
import com.weather.transform.ImagePreprocessor

import java.io.File

object Main {

  def main(args: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "C:/hadoop")

    println("=== MICROSERVICE 2 ===")

    // 1. Lire le fichier parquet avec Spark
    val parquetPath = "C:/Users/akric/Downloads/weather-project/weather-project/Weather-Image-Recognition/weather.parquet"
    println("\n[ÉTAPE 1] Lecture du fichier Parquet...")
    ParquetReader.readParquet(parquetPath)

    // 2. Prétraiter l'image avec le code du Microservice 1
    val testImage = new File("C:/spark-data/dataset/rain/1800.jpg")
    val resizedImage = new File("C:/spark-data/dataset/temp_resized.jpg")

    println(s"\n[ÉTAPE 2] Prétraitement avec Microservice 1 : ${testImage.getName}")
    ImagePreprocessor.resizeImage(testImage, resizedImage)
    println(s"  Image resizée : 224x224 → ${resizedImage.getAbsolutePath}")

    // 3. Faire la prédiction via l'API Python
    println("\n[ÉTAPE 3] Prédiction via API Python...")
    val (label, confidence, allPredictions) = PredictionService.predict(resizedImage)
    println(s"  Résultat : $label ($confidence%)")

    // 4. Sauvegarder en BDD
    println("\n[ÉTAPE 4] Sauvegarde en BDD...")
    DatabaseManager.savePrediction(
      imageName = testImage.getName,
      predictedLabel = label,
      confidence = confidence,
      allPredictions = allPredictions,
      modelName = "ResNet50",
      modelAccuracy = 98.0
    )

    // 5. Afficher l'historique
    DatabaseManager.getAllPredictions()

    // Nettoyer
    if (resizedImage.exists()) resizedImage.delete()

    // 6. Lancer le serveur HTTP (reste allumé pour Streamlit)
    println("\n[ÉTAPE 6] Lancement du serveur HTTP...")
    PredictionServer.start()

    println("=== MICROSERVICE 2 PRÊT ===")
    println("En attente de requêtes sur le port 9001...")

    // Garder le programme en vie
    Thread.currentThread().join()
  }
}