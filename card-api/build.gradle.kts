import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test

plugins {
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":card-common"))
    implementation(project(":card-core"))
    implementation(project(":card-infra"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.6")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.yaml:snakeyaml")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

val testSourceSet = the<SourceSetContainer>()["test"]

tasks.register<Test>("postgresIntegrationTest") {
    description = "Runs the PostgreSQL/Testcontainers integration flow."
    group = "verification"
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("com.jean202.cardmizer.api.PostgresIntegrationTest")
    }
    shouldRunAfter(tasks.named<Test>("test"))
}
