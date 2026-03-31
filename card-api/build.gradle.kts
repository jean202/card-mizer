plugins {
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
    java
}

dependencies {
    implementation(project(":card-common"))
    implementation(project(":card-core"))
    implementation(project(":card-infra"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
