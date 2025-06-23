import sbtwelcome.*
import scala.sys.process.*

val ZIO_MAELSTROM_VERSION = "2.2.0+10-ca9a3d28-SNAPSHOT"
Global / onChangedBuildSource := ReloadOnSourceChanges

// Challenge #1: Echo
lazy val echo = project
  .in(file("1-echo"))
  .settings(
    agentParams := "-w echo --node-count 1 --time-limit 3",
    testParams := "-w echo --node-count 1 --time-limit 10"
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings*)

// Challenge #2: Unique ID Generation
lazy val `unique-id-generation` = project
  .in(file("2-unique-id-generation"))
  .settings(
    agentParams := "-w unique-ids --node-count 1 --time-limit 3 --rate 3",
    testParams := "-w unique-ids --time-limit 30 --rate 1000 --node-count 3 --availability total --nemesis partition"
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings*)

// Challenge #3a: Single-Node Broadcast
lazy val `single-node-broadcast` = project
  .in(file("3a-single-node-broadcast"))
  .settings(
    agentParams := "-w broadcast --node-count 1 --time-limit 3 --rate 3",
    testParams := "-w broadcast --node-count 1 --time-limit 20 --rate 10"
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings*)

// Challenge #3b: Multi-Node Broadcast
lazy val `multi-node-broadcast` = project
  .in(file("3b-multi-node-broadcast"))
  .settings(
    agentParams := "-w broadcast --node-count 1 --time-limit 3 --rate 3",
    testParams := "-w broadcast --node-count 5 --time-limit 20 --rate 10"
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings*)

// Challenge #3c: Fault-Tolerant Broadcast
lazy val `fault-tolerant-broadcast` = project
  .in(file("3c-fault-tolerant-broadcast"))
  .settings(
    agentParams := "-w broadcast --node-count 1 --time-limit 3 --rate 3 --nemesis partition",
    testParams := "-w broadcast --node-count 5 --time-limit 20 --rate 10 --nemesis partition"
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings*)

// Challenge #3d: Efficient Broadcast, Part I
lazy val `efficient-broadcast-1` = project
  .in(file("3d-efficient-broadcast-1"))
  .settings(
    agentParams := "-w broadcast --node-count 1 --time-limit 3 --rate 3 --latency 100",
    testParams := "-w broadcast --node-count 25 --time-limit 20 --rate 100 --latency 100"
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings*)

// Challenge #3e: Efficient Broadcast, Part II
lazy val `efficient-broadcast-2` = project
  .in(file("3e-efficient-broadcast-2"))
  .settings(
    agentParams := "-w broadcast --node-count 1 --time-limit 3 --rate 3 --latency 100",
    testParams := "-w broadcast --node-count 25 --time-limit 20 --rate 100 --latency 100"
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings*)

// Challenge #4: Grow-Only Counter
lazy val `grow-only-counter` = project
  .in(file("4-grow-only-counter"))
  .settings(
    agentParams := "-w g-counter --node-count 1 --time-limit 3 --rate 3",
    testParams := "-w g-counter --node-count 3 --rate 100 --time-limit 20 --nemesis partition"
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings*)

// Challenge #5a: Single-Node Kafka-Style Log
lazy val `kafka-style-log` = project
  .in(file("5a-kafka-style-log"))
  .settings(
    agentParams := "-w kafka --node-count 1 --concurrency 2n --time-limit 5 --rate 3",
    testParams := "-w kafka --node-count 1 --concurrency 2n --time-limit 20 --rate 1000"
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings*)

// Challenge #5b: Multi-Node Kafka-Style Log
lazy val `multi-node-kafka-style-log` = project
  .in(file("5b-multi-node-kafka-style-log"))
  .settings(
    agentParams := "-w kafka --node-count 1 --concurrency 2n --time-limit 5 --rate 3",
    testParams := "-w kafka --node-count 2 --concurrency 2n --time-limit 20 --rate 1000"
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings*)

// Challenge #5c: Efficient Kafka-Style Log
lazy val `efficient-kafka-style-log` = project
  .in(file("5c-efficient-kafka-style-log"))
  .settings(
    agentParams := "-w kafka --node-count 1 --concurrency 2n --time-limit 5 --rate 3",
    testParams := "-w kafka --node-count 2 --concurrency 2n --time-limit 20 --rate 1000"
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings*)

// Challenge #6a: Single-Node, Totally-Available Transactions
lazy val `single-node-totally-available-transactions` = project
  .in(file("6a-single-node-totally-available-transactions"))
  .settings(
    agentParams := "-w txn-rw-register --node-count 1 --time-limit 5 --rate 3 --concurrency 2n --consistency-models read-uncommitted --availability total",
    testParams := "-w txn-rw-register --node-count 1 --time-limit 20 --rate 1000 --concurrency 2n --consistency-models read-uncommitted --availability total"
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings*)

// Challenge #6b: Totally-Available, Read Uncommitted Transactions
lazy val `totally-available-read-uncommitted-transactions` = project
  .in(file("6b-totally-available-read-uncommitted-transactions"))
  .settings(
    agentParams := "-w txn-rw-register --node-count 1 --concurrency 2n --time-limit 5 --rate 3 --consistency-models read-uncommitted  --availability total",
    testParams := "-w txn-rw-register --node-count 2 --concurrency 2n --time-limit 20 --rate 1000 --consistency-models read-uncommitted --availability total --nemesis partition"
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings*)

// Challenge #6c: Totally-Available, Read Committed Transactions
lazy val `totally-available-read-committed-transactions` = project
  .in(file("6c-totally-available-read-committed-transactions"))
  .settings(
    agentParams := "-w txn-rw-register --node-count 1 --concurrency 2n --time-limit 5 --rate 3 --consistency-models read-committed --availability total",
    testParams := "-w txn-rw-register --node-count 2 --concurrency 2n --time-limit 20 --rate 1000 --consistency-models read-committed --availability total --nemesis partition"
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings*)

// --------------- SETUP ---------------
logo := ""

lazy val platformSuffix: String = {
  val os: String = System.getProperty("os.name") match {
    case x if x.toLowerCase.contains("mac")   => "darwin"
    case x if x.toLowerCase.contains("linux") => "linux"
    case x if x.toLowerCase.contains("win")   => "windows"
    case x                                    => throw new RuntimeException("could not detect os: " + x)
  }
  val arch = System.getProperty("os.arch")
  s"$os-$arch"
}

lazy val commonSettings = Seq(
  scalaVersion := "3.7.1",
  organization := "com.gossip-glomers",
  Compile / mainClass := Some("gossipGlomers.Main"),
  resolvers += Resolver.sonatypeCentralSnapshots,
  scalacOptions += "-Wunused:all",
  version := "0.1.0-SNAPSHOT",
  run / connectInput := true,
  nativeImageGraalHome := (sbt.io.Path.userHome / ".sdkman/candidates/java/current/").toPath,
  nativeImageOptions ++= Seq("--no-fallback", "-march=native", "-Ob"), // , "--verbose"),
  nativeImageAgentOutputDir := baseDirectory.value / "src" / "main" / "resources" / "META-INF" / "native-image",
  nativeImageAgentMerge := false,
  nativeImageInstalled := true,
  libraryDependencies += "com.bilal-fazlani" %% "zio-maelstrom" % ZIO_MAELSTROM_VERSION,
  nativeImageOutput := target.value / s"${name.value}-$platformSuffix",
  logo := "",
  bootstrap := {
    publishLocal.value
    val exitCode = Process(
      Seq(
        "coursier",
        "bootstrap",
        "-r",
        "sonatype:snapshots",
        "--standalone",
        s"com.gossip-glomers:${name.value}_3:0.1.0-SNAPSHOT",
        "-f",
        "-o",
        s"${target.value}/${name.value}.jar"
      )
    ).!
    if (exitCode != 0) {
      throw new RuntimeException("bootstrap failed")
    }
    val scriptContents = s"""#!/usr/bin/env bash
                                |set -e
                                |set -x
                                |
                                |maelstrom test --bin $$(dirname "$$0")/${name.value}.jar ${testParams.value}
                                |""".stripMargin
    val p = target.value / "jvm-simulation.sh"
    io.IO.write(p, scriptContents)
    io.IO.chmod("rwxr-----", p)
  },
  maelstromRunAgent := {
    bootstrap.value
    exec("maelstrom test --bin run.sh " + agentParams.value, baseDirectory.value, name.value)
    stopNativeImageAgent(baseDirectory.value)
  },
  usefulTasks := Seq(
    UsefulTask(
      "bootstrap",
      "creates fat jar with all dependencies with a maelstrom runner script"
    ),
    UsefulTask(
      "maelstromRunAgent",
      "run maelstrom simulation to generate graalvm reflection configuration"
    ),
    UsefulTask(
      "nativePackage",
      "create native image with a maelstrom runner script"
    )
  ),
  nativePackage := {
    nativeImage.value
    val scriptContents = s"""#!/usr/bin/env bash
                            |set -e
                            |set -x
                            |
                            |maelstrom test --bin $$(dirname "$$0")/${name.value}-$platformSuffix ${testParams.value}
                            |""".stripMargin
    val p = target.value / "native-simulation.sh"
    io.IO.write(p, scriptContents)
    io.IO.chmod("rwxr-----", p)
  }
)

lazy val nativePackage = taskKey[Unit]("create native image with maelstrom runner script")
lazy val agentParams = settingKey[String]("agent parameters")
lazy val testParams = settingKey[String]("test parameters")

lazy val bootstrap = taskKey[Unit]("Create a fat jar file")

lazy val maelstromRunAgent =
  taskKey[Unit]("run maelstrom simulation to generate graalvm agent configuration")

def exec(str: String, projectDir: File, projectName: String) = {
  val exitCode = Process(
    str,
    file("."),
    "PROJECT_DIR" -> projectDir.toPath.toAbsolutePath.toString,
    "PROJECT_NAME" -> projectName
  ).!
  if (exitCode != 0) {
    throw new Exception(s"Failed to run $str")
  }
}

def stopNativeImageAgent(projectDir: File) = {
  Process(
    "sh stop.sh",
    file("."),
    "PROJECT_DIR" -> projectDir.toPath.toAbsolutePath.toString,
  ).!!
}

// -------------------------------------
