import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.6.10"
}

group = "co.selim"
version = "1.0.0"

repositories {
  mavenCentral()
}

val vertxVersion = "4.2.4"

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-web")
  implementation(kotlin("stdlib-jdk8"))

  implementation("org.slf4j:slf4j-api:1.7.35")
  testRuntimeOnly("org.slf4j:slf4j-simple:1.7.35")

  testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("com.konghq:unirest-java:3.13.6:standalone")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "11"

kotlin {
  explicitApi()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}
