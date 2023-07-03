import sbtwelcome._
import scala.sys.process._

logo := ""

lazy val commonSettings = Seq(
  scalaVersion := "3.3.0",
  organization := "com.gossip-glomers",
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
  scalacOptions += "-Wunused:all",
  version := "0.1.0-SNAPSHOT",
  run / connectInput := true,
  nativeImageGraalHome := file(
    "/Library/Java/JavaVirtualMachines/graalvm-community-openjdk-20.0.1+9.1/Contents/Home"
  ).toPath,
  nativeImageAgentOutputDir := baseDirectory.value / "src" / "main" / "resources" / "META-INF" / "native-image",
  nativeImageAgentMerge := true,
  nativeImageInstalled := true,
  libraryDependencies += "com.bilal-fazlani" %% "zio-maelstrom" % "0.4.1",
  nativeImageOutput := baseDirectory.value / name.value,
  logo := "",
  bootstrap := {
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
)

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

lazy val efficientBroadcast1 = project
  .in(file("./efficient-broadcast-1"))
  .settings(
    name := "efficient-broadcast-1",
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
  .settings(commonSettings: _*)

lazy val efficientBroadcast2 = project
  .in(file("./efficient-broadcast-2"))
  .settings(
    name := "efficient-broadcast-2",
    Compile / mainClass := Some("gossipGlomers.EfficientBroadcast2"),
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
  .settings(commonSettings: _*)
