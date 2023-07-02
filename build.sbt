import sbtwelcome._
import scala.sys.process._

ThisBuild / scalaVersion := "3.3.0"
ThisBuild / organization := "com.gossip-glomers"
ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")
ThisBuild / scalacOptions += "-Wunused:all"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / run / connectInput := true
ThisBuild / nativeImageGraalHome := file(
  "/Library/Java/JavaVirtualMachines/graalvm-community-openjdk-20.0.1+9.1/Contents/Home"
).toPath
ThisBuild / nativeImageAgentOutputDir := baseDirectory.value / "src" / "main" / "resources" / "META-INF" / "native-image"
ThisBuild / nativeImageAgentMerge := true
ThisBuild / nativeImageInstalled := true

logo := ""

usefulTasks := Seq(
  UsefulTask(
    "publishLocal;bootstrap",
    "Create a fat jar file"
  ),
  UsefulTask(
    "runSimulation",
    "run maelstrom simulation to generate graalvm agent configuration"
  )
)

lazy val bootstrap = taskKey[Unit]("Create a fat jar file")
lazy val runSimulation = taskKey[Unit]("run maelstrom simulation to generate graalvm agent configuration")

ThisBuild / bootstrap := {
  Process(
    Seq(
      "coursier",
      "bootstrap",
      "--standalone",
      s"com.gossip-glomers:${name.value}_3:0.1.0-SNAPSHOT",
      "-f",
      "-o",
      s"${name.value}.jar"
    )
  ).!
}

lazy val efficientBroadcast1 = project
  .in(file("./efficient-broadcast-1"))
  .settings(
    name := "efficient-broadcast-1",
    libraryDependencies += "com.bilal-fazlani" %% "zio-maelstrom" % "0.4.1",
    nativeImageOutput := baseDirectory.value / name.value,
    Compile / mainClass := Some("gossipGlomers.EfficientBroadcast1"),
    runSimulation := {
      Process(
        Seq(
          "maelstrom",
          "test",
          "-w",
          "broadcast",
          "--bin",
          s"run.sh ${baseDirectory.value}/${name.value}.jar",
          "--node-count",
          "1",
          "--time-limit",
          "5",
          "--rate",
          "10"
        )
      ).!
    }
  )
  .enablePlugins(NativeImagePlugin)
