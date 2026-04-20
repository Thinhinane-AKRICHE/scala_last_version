package com.weather.domain

case class TempImageRecord(
    imagePath: String,
    fileName: String,
    label: String,
    labelId: Int,
    width: Int,
    height: Int,
    channels: Int,
    features: Seq[Float]
)