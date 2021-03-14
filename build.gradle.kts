plugins {
    kotlin("jvm") version "1.4.31" apply false
}

allprojects {
    group = "top.anagke"
    version = "0.2.0"
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/spring") }
        maven { url = uri("https://maven.aliyun.com/repository/spring-plugin") }
        jcenter()
        mavenCentral()
    }

    tasks {
        named<Test>("test") {
            useJUnitPlatform()
        }
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "6.8.3"
}