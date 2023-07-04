import sbtwelcome._
import scala.sys.process._

logo := ""

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
  libraryDependencies += "com.bilal-fazlani" %% "zio-maelstrom" % "0.4.1",
  nativeImageOutput := file(name.value) / "target" / (name.value + "-darwin-x86_64"),
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
    ).!
  },
  usefulTasks := Seq(
    UsefulTask(
      "maelstromRunAgent",
      "run maelstrom simulation to generate graalvm reflection configuration"
    ),
    UsefulTask(
      "makeNativeImage",
      "create native image"
    )
  )
)

addCommandAlias("makeNativeImage", ";bootstrap;nativeImage")

lazy val bootstrap = taskKey[Unit]("Create a fat jar file")

lazy val maelstromRunAgent =
  taskKey[Unit]("run maelstrom simulation to generate graalvm agent configuration")

lazy val `efficient-broadcast-1` = project
  .in(file("efficient-broadcast-1"))
  .settings(
    maelstromRunAgent := {
      bootstrap.value
      exec("maelstrom test -w broadcast --bin run.sh --node-count 1 --time-limit 3 --rate 2", name.value)
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
      exec("maelstrom test -w broadcast --bin run.sh --node-count 1 --time-limit 3 --rate 2", name.value)
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
).!

def stopNativeImageAgent(name: String) = {
  Process(
    "sh stop.sh",
    file("."),
    "BASE_PATH" -> file("").toPath.toAbsolutePath.toString,
    "PROJECT_NAME" -> name
  ).!
}
