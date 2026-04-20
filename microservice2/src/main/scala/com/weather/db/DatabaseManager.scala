package com.weather.db

import java.sql.{Connection, DriverManager, PreparedStatement, Timestamp}
import java.time.LocalDateTime

object DatabaseManager {

  private val url = "jdbc:postgresql://localhost:5432/weather_db"
  private val user = "postgres"
  private val password = "1234"

  def getConnection(): Connection = {
    DriverManager.getConnection(url, user, password)
  }

  def savePrediction(
    imageName: String,
    predictedLabel: String,
    confidence: Double,
    allPredictions: String,
    modelName: String,
    modelAccuracy: Double
  ): Unit = {
    val conn = getConnection()
    try {
      val sql = """
        INSERT INTO predictions (image_name, predicted_label, confidence, 
                                  all_predictions, model_name, model_accuracy)
        VALUES (?, ?, ?, ?, ?, ?)
      """
      val stmt = conn.prepareStatement(sql)
      stmt.setString(1, imageName)
      stmt.setString(2, predictedLabel)
      stmt.setDouble(3, confidence)
      stmt.setString(4, allPredictions)
      stmt.setString(5, modelName)
      stmt.setDouble(6, modelAccuracy)
      stmt.executeUpdate()
      stmt.close()
      println(s"[DB] Prédiction sauvegardée : $imageName → $predictedLabel ($confidence%)")
    } finally {
      conn.close()
    }
  }

  def getAllPredictions(): Unit = {
    val conn = getConnection()
    try {
      val stmt = conn.createStatement()
      val rs = stmt.executeQuery("SELECT * FROM predictions ORDER BY created_at DESC")
      println("\n=== HISTORIQUE DES PRÉDICTIONS ===")
      while (rs.next()) {
        println(s"  ${rs.getTimestamp("created_at")} | ${rs.getString("image_name")} | ${rs.getString("predicted_label")} | ${rs.getDouble("confidence")}%")
      }
      rs.close()
      stmt.close()
    } finally {
      conn.close()
    }
  }
}