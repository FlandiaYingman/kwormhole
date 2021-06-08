plugins {
    kotlin("jvm")

    id("com.github.ben-manes.versions") version "0.36.0"
}

dependencies {
    implementation(project(":kwormhole-core"))

    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("top.anagke:kio:0.1.0")

    implementation("com.xenomachina:kotlin-argparser:2.0.7")
    implementation("io.github.microutils:kotlin-logging:2.0.4")
    implementation("ch.qos.logback:logback-classic:1.3.0-alpha5")

    implementation("no.tornado:tornadofx:1.7.20")

    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("com.squareup.okhttp3:okhttp-sse:4.9.1")
    implementation("com.squareup.moshi:moshi:1.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.12.0")

    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.1")

    implementation("com.google.code.gson:gson:2.8.6")

    testImplementation("io.ktor:ktor-server-core:1.5.1")
    testImplementation("io.ktor:ktor-server-cio:1.5.1")
    testImplementation("io.ktor:ktor-websockets:1.5.1")

    implementation("org.jetbrains.exposed:exposed-core:0.29.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.29.1")
    runtimeOnly("org.jetbrains.exposed:exposed-jdbc:0.29.1")
    implementation("com.zaxxer:HikariCP:4.0.1")
    implementation("org.xerial:sqlite-jdbc:3.34.0")
    implementation("org.greenrobot:eventbus:3.2.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
}