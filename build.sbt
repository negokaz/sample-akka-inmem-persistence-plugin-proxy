name := "sample-akka-inmem-persistence-plugin-proxy"

version := "1.0"

scalaVersion := "2.13.1"

lazy val akkaVersion = "2.6.10"

resolvers += Resolver.bintrayRepo("dnvriend", "maven")

libraryDependencies ++= Seq(
  "com.typesafe.akka"   %% "akka-actor-typed"            % akkaVersion,
  "ch.qos.logback"       % "logback-classic"             % "1.2.3",
  "com.typesafe.akka"   %% "akka-persistence-typed"      % akkaVersion,
  "com.typesafe.akka"   %% "akka-cluster-typed"          % akkaVersion,
  "com.typesafe.akka"   %% "akka-cluster-sharding-typed" % akkaVersion,
  "com.github.dnvriend" %% "akka-persistence-inmemory"   % "2.5.15.2",
  "com.typesafe.akka"   %% "akka-actor-testkit-typed"    % akkaVersion % Test,
  "org.scalatest"       %% "scalatest"                   % "3.1.0"     % Test,
  "com.typesafe.akka"   %% "akka-multi-node-testkit"     % akkaVersion % Test,
)

enablePlugins(MultiJvmPlugin)

configs(MultiJvm)
