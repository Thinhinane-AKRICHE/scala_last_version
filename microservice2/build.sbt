ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.7"

lazy val microservice1 = RootProject(file("../weather-recognition"))

lazy val root = (project in file("."))
  .dependsOn(microservice1)
  .settings(
    name := "weather-microservice2",
    libraryDependencies ++= Seq(
      "org.postgresql" % "postgresql" % "42.7.1",
      "org.apache.spark" % "spark-core_2.13" % "3.5.1",
      "org.apache.spark" % "spark-sql_2.13" % "3.5.1"
    ),
    dependencyOverrides += "com.github.luben" % "zstd-jni" % "1.5.5-4"
  )