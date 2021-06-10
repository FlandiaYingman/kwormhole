plugins {
    kotlin("jvm")
    kotlin("plugin.spring") version "1.5.10"
    kotlin("plugin.jpa") version "1.5.10"

    id("org.springframework.boot") version "2.5.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"

    id("com.github.ben-manes.versions") version "0.39.0"
}


dependencies {
    // Core: KWormhole Core
    implementation(project(":kwormhole-core"))

    // Language: Kotlin Stdlib
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    // Spring Boot Framework
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.h2database:h2")

    // Util: Other Util Libraries
    implementation("com.google.code.gson:gson:2.8.7")
    implementation("io.github.microutils:kotlin-logging:2.0.8")
    implementation("top.anagke:kio:0.1.0")


    // Integration Test: Client
    testImplementation(project(":kwormhole-client"))
    testImplementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.2")

    // Test Spring Boot Framework
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}