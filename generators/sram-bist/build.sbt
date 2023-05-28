name := "sram-bist"
version := "0.1"
scalaVersion := "2.13.10"

libraryDependencies ++= Seq(
  "edu.berkeley.cs" %% "chiseltest" % "0.5.4",
  "edu.berkeley.cs" %% "rocketchip" % "1.2.+")
