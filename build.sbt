name := "scorecard-data-loader"

scalaVersion := "2.11.8"

lazy val `scorecard-data-loader` = (project in file("."))
  .enablePlugins(GitVersioning)
  .enablePlugins(GitBranchPrompt)

git.useGitDescribe := true

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.7",
  "org.joda" % "joda-convert" % "1.7",
  "org.typelevel" %% "cats-core" % "0.7.0",
  "com.typesafe.play" %% "play-json" % "2.5.4",
  "com.wellfactored" %% "play-bindings" % "1.1.0",
  "org.reactivemongo" %% "reactivemongo" % "0.11.14",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.3",

  "org.scalatest" %% "scalatest" % "3.0.0" % Test
)

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.7.1")
