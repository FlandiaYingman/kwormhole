plugins {
    kotlin("jvm")

    id("com.github.ben-manes.versions") version "0.39.0"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.lz4:lz4-java:1.7.1")

    api("com.squareup.okio:okio:3.0.0-alpha.6")
    api("top.anagke:kio:1.1.0")
}

tasks.test {
    useJUnitPlatform()
}