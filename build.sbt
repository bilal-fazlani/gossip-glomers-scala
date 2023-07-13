import sbtwelcome._
import scala.sys.process._

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

val ZIO_MAELSTROM_VERSION = "0.6.0"

lazy val commonSettings = Seq(
  scalaVersion := "3.3.0",
  organization := "com.gossip-glomers",
  Compile / mainClass := Some("gossipGlomers.Main"),
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
  scalacOptions += "-Wunused:all",
  version := "0.1.0-SNAPSHOT",
  run / connectInput := true,
  nativeImageGraalHome := file(
    "/Library/Java/JavaVirtualMachines/graalvm-community-openjdk-20.0.1+9.1/Contents/Home"
  ).toPath,
  nativeImageAgentOutputDir := baseDirectory.value / "src" / "main" / "resources" / "META-INF" / "native-image",
  nativeImageAgentMerge := false,
  nativeImageOptions ++= Seq("--no-fallback", "-march=native"), // , "--verbose"),
  nativeImageInstalled := true,
  libraryDependencies += "com.bilal-fazlani" %% "zio-maelstrom" % ZIO_MAELSTROM_VERSION,
  nativeImageOutput := target.value / s"${name.value}-$platformSuffix",
  logo := "",
  bootstrap := {
    publishLocal.value
    Process(
      Seq(
        "coursier",
        "bootstrap",
        "--standalone",
        s"com.gossip-glomers:${name.value}_3:0.1.0-SNAPSHOT",
        "-f",
        "-o",
        s"${name.value}/target/${name.value}.jar"
      )
    ).!!
  },
  usefulTasks := Seq(
    UsefulTask(
      "maelstromRunAgent",
      "run maelstrom simulation to generate graalvm reflection configuration"
    ),
    UsefulTask(
      "nativeImage",
      "create native image"
    )
  )
)

lazy val bootstrap = taskKey[Unit]("Create a fat jar file")

lazy val maelstromRunAgent =
  taskKey[Unit]("run maelstrom simulation to generate graalvm agent configuration")

lazy val echo = project
  .in(file("echo"))
  .settings(
    maelstromRunAgent := {
      bootstrap.value
      exec("maelstrom test -w echo --bin run.sh --node-count 1 --time-limit 3 --rate 3", name.value)
      stopNativeImageAgent(name.value)
    }
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings: _*)

lazy val `unique-id-generation` = project
  .in(file("unique-id-generation"))
  .settings(
    maelstromRunAgent := {
      bootstrap.value
      exec("maelstrom test -w unique-ids --bin run.sh --node-count 1 --time-limit 3 --rate 3", name.value)
      stopNativeImageAgent(name.value)
    }
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings: _*)

lazy val `single-node-broadcast` = project
  .in(file("single-node-broadcast"))
  .settings(
    maelstromRunAgent := {
      bootstrap.value
      exec("maelstrom test -w broadcast --bin run.sh --node-count 1 --time-limit 3 --rate 3", name.value)
      stopNativeImageAgent(name.value)
    }
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings: _*)

lazy val `multi-node-broadcast` = project
  .in(file("multi-node-broadcast"))
  .settings(
    maelstromRunAgent := {
      bootstrap.value
      exec("maelstrom test -w broadcast --bin run.sh --node-count 1 --time-limit 3 --rate 3", name.value)
      stopNativeImageAgent(name.value)
    }
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings: _*)

lazy val `fault-tolerant-broadcast` = project
  .in(file("fault-tolerant-broadcast"))
  .settings(
    maelstromRunAgent := {
      bootstrap.value
      exec("maelstrom test -w broadcast --bin run.sh --node-count 1 --time-limit 3 --rate 3", name.value)
      stopNativeImageAgent(name.value)
    }
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings: _*)

lazy val `efficient-broadcast-1` = project
  .in(file("efficient-broadcast-1"))
  .settings(
    maelstromRunAgent := {
      bootstrap.value
      exec("maelstrom test -w broadcast --bin run.sh --node-count 1 --time-limit 3 --rate 3", name.value)
      stopNativeImageAgent(name.value)
    }
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings: _*)

lazy val `efficient-broadcast-2` = project
  .in(file("efficient-broadcast-2"))
  .settings(
    maelstromRunAgent := {
      bootstrap.value
      exec("maelstrom test -w broadcast --bin run.sh --node-count 1 --time-limit 3 --rate 3", name.value)
      stopNativeImageAgent(name.value)
    }
  )
  .enablePlugins(NativeImagePlugin)
  .settings(commonSettings: _*)

def exec(str: String, name: String) = Process(
  str,
  file("."),
  "BASE_PATH" -> file("").toPath.toAbsolutePath.toString,
  "PROJECT_NAME" -> name
).!!

def stopNativeImageAgent(name: String) = {
  Process(
    "sh stop.sh",
    file("."),
    "BASE_PATH" -> file("").toPath.toAbsolutePath.toString,
    "PROJECT_NAME" -> name
  ).!
}
