plugins {
    kotlin("jvm")
    kotlin("plugin.spring") version "1.4.31"

    id("org.springframework.boot") version "2.4.1"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
}


dependencies {
    implementation(project(":kwormhole-core"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("org.junit.jupiter:junit-jupiter:5.4.2")

    runtimeOnly("mysql:mysql-connector-java")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation(kotlin("stdlib"))


    implementation("io.github.microutils:kotlin-logging:2.0.3")
    implementation("org.lz4:lz4-java:1.7.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}