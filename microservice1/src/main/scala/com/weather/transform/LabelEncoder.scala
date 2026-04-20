package com.weather.transform

object LabelEncoder {
  def buildLabelMap(labels: Seq[String]): Map[String, Int] = {
    labels.distinct.sorted.zipWithIndex.toMap
  }
}