plugins {
    java
    kotlin("jvm") version "1.4.21"
    id("com.github.ben-manes.versions") version "0.36.0"
}

group = "top.anagke"
version = "0.1.0"

repositories {
    maven { url = uri("https://maven.aliyun.com/repository/central") }
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

    implementation("com.xenomachina:kotlin-argparser:2.0.7")
    implementation("io.github.microutils:kotlin-logging:2.0.4")

    implementation("no.tornado:tornadofx:1.7.20")
    implementation("io.ktor:ktor-client-core:1.5.0")
    implementation("io.ktor:ktor-client-cio:1.5.0")
    implementation("io.ktor:ktor-client-json:1.5.0")
    implementation("io.ktor:ktor-client-gson:1.5.0")
    implementation("io.ktor:ktor-client-mock:1.5.0")
    implementation("io.ktor:ktor-server-core:1.5.0")
    implementation("io.ktor:ktor-server-cio:1.5.0")
    implementation("io.ktor:ktor-gson:1.5.0")

    implementation("org.jetbrains.exposed:exposed-core:0.29.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.29.1")
    runtimeOnly("org.jetbrains.exposed:exposed-jdbc:0.29.1")
    implementation("com.zaxxer:HikariCP:4.0.1")
    implementation("org.xerial:sqlite-jdbc:3.34.0")
    implementation("org.lz4:lz4-java:1.7.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}