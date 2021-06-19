plugins {
    kotlin("jvm")

    id("com.github.ben-manes.versions") version "0.39.0"
}

dependencies {
    // Core: KWormhole Core
    implementation(project(":kwormhole-core"))

    // Language: Kotlin Stdlib
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    // Logging: Kotlin Logging / Slf4j / Logback
    implementation("io.github.microutils:kotlin-logging:2.0.8")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("ch.qos.logback:logback-classic:1.3.0-alpha5")

    // ORM: Exposed / HiKariCP
    implementation("org.jetbrains.exposed:exposed-core:0.32.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.32.1")
    runtimeOnly("org.jetbrains.exposed:exposed-jdbc:0.32.1")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.xerial:sqlite-jdbc:3.34.0")

    // Http: OkHttp
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.2")

    // Util: Other Util Libraries
    implementation("top.anagke:kio:1.0.0")
    implementation("com.google.code.gson:gson:2.8.7")
    implementation("com.xenomachina:kotlin-argparser:2.0.7")


    // Test Framework: JUnit
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")

    // Test Server Implementation: Ktor
    testImplementation("io.ktor:ktor-server-core:1.6.0")
    testImplementation("io.ktor:ktor-server-cio:1.6.0")
    testImplementation("io.ktor:ktor-websockets:1.6.0")

    // Test Server Implementation: OkHttp Mock Web Server
    testImplementation("com.squareup.okhttp3:mockwebserver:5.0.0-alpha.2")
}

tasks.test {
    useJUnitPlatform()
}