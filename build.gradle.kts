plugins {
    kotlin("jvm") version "1.5.10" apply(false)
}

allprojects {
    group = "top.anagke"
    version = "0.2.0"
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }
}

tasks.wrapper {
    gradleVersion = "7.0.2"
}