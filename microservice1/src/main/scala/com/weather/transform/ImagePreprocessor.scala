package com.weather.transform

import java.awt.{Graphics2D, Image}
import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, File}
import javax.imageio.ImageIO

object ImagePreprocessor {

  def preprocessBytes(
      content: Array[Byte],
      targetWidth: Int = 64,
      targetHeight: Int = 64
  ): (Int, Int, Int, Seq[Float]) = {
    val original = ImageIO.read(new ByteArrayInputStream(content))

    if (original == null) {
      throw new IllegalArgumentException("Impossible de lire l'image depuis les bytes")
    }

    val resized = resizeToRgb(original, targetWidth, targetHeight)
    val features = flattenNormalizedRgb(resized)

    (targetWidth, targetHeight, 3, features)
  }

  def resizeBytesToFile(
      content: Array[Byte],
      outputFile: File,
      targetWidth: Int = 224,
      targetHeight: Int = 224
  ): File = {
    val original = ImageIO.read(new ByteArrayInputStream(content))

    if (original == null) {
      throw new IllegalArgumentException("Impossible de lire l'image depuis les bytes")
    }

    val resized = resizeToRgb(original, targetWidth, targetHeight)
    ImageIO.write(resized, "jpg", outputFile)
    outputFile
  }

  def resizeImage(
      imageFile: File,
      outputFile: File,
      targetWidth: Int = 224,
      targetHeight: Int = 224
  ): File = {
    val original = ImageIO.read(imageFile)

    if (original == null) {
      throw new IllegalArgumentException(
        s"Impossible de lire l'image : ${imageFile.getAbsolutePath}"
      )
    }

    val resized = resizeToRgb(original, targetWidth, targetHeight)
    ImageIO.write(resized, "jpg", outputFile)
    outputFile
  }

  private def resizeToRgb(
      input: BufferedImage,
      width: Int,
      height: Int
  ): BufferedImage = {
    val scaled: Image = input.getScaledInstance(width, height, Image.SCALE_SMOOTH)
    val output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

    val g2d: Graphics2D = output.createGraphics()
    try {
      g2d.drawImage(scaled, 0, 0, null)
    } finally {
      g2d.dispose()
    }

    output
  }

  private def flattenNormalizedRgb(image: BufferedImage): Seq[Float] = {
    val width = image.getWidth
    val height = image.getHeight

    val buffer = scala.collection.mutable.ArrayBuffer[Float]()
    buffer.sizeHint(width * height * 3)

    for {
      y <- 0 until height
      x <- 0 until width
    } {
      val rgb = image.getRGB(x, y)
      val r = ((rgb >> 16) & 0xFF) / 255.0f
      val g = ((rgb >> 8) & 0xFF) / 255.0f
      val b = (rgb & 0xFF) / 255.0f

      buffer += r
      buffer += g
      buffer += b
    }

    buffer.toSeq
  }
}