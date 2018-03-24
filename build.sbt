name := "bingle"

version := "1.0"

scalaVersion := "2.12.1"

organizationName := "com.aisle_n"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"   % "10.1.0-RC2",
  "com.typesafe.akka" %% "akka-slf4j"      % "2.5.9",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-persistence" % "2.5.9",
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.57",
  "com.typesafe.akka" %% "akka-persistence-query" % "2.5.9",
  "com.typesafe.akka" %% "akka-cluster-sharding" %  "2.5.11"
)