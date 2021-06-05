plugins {
    kotlin("jvm")
    kotlin("plugin.spring") version "1.4.32"
    kotlin("plugin.jpa") version "1.4.32"

    id("org.springframework.boot") version "2.4.1"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
}


dependencies {
    implementation(project(":kwormhole-core"))

    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    implementation("com.google.code.gson:gson:2.8.6")
    implementation("io.github.microutils:kotlin-logging:2.0.3")

    runtimeOnly("com.h2database:h2")
}