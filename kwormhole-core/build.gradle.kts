plugins {
    kotlin("jvm")

    id("com.github.ben-manes.versions") version "0.39.0"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.lz4:lz4-java:1.7.1")
}

tasks.test {
    useJUnitPlatform()
}