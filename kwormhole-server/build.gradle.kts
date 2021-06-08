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

    testImplementation("top.anagke:kio:0.1.0")
    testImplementation(project(":kwormhole-client"))
    testImplementation("com.squareup.okhttp3:okhttp:4.9.1")
    testImplementation("com.squareup.okhttp3:okhttp-sse:4.9.1")
    testImplementation("com.squareup.moshi:moshi:1.12.0")
    testImplementation("com.squareup.moshi:moshi-kotlin:1.12.0")
}