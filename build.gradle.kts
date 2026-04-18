plugins {
    base
    kotlin("jvm") version "2.2.20" apply false
    kotlin("plugin.spring") version "2.2.20" apply false
    kotlin("plugin.jpa") version "2.2.20" apply false
}

group = "io.github.jean202.cardmizer"
version = "0.1.0-SNAPSHOT"

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(17)
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict")
        }
    }

    dependencies {
        add("implementation", "org.jetbrains.kotlin:kotlin-reflect")
        add("testImplementation", platform("org.junit:junit-bom:5.12.1"))
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
