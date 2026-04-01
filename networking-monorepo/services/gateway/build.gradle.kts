plugins {
    id("java")
    alias(libs.plugins.spring.boot)
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    // Spring boot
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.starter.actuator)
    testImplementation(libs.spring.boot.starter.test)

    implementation(platform(libs.spring.cloud.bom))
    implementation(libs.spring.cloud.starter.gateway)
    implementation(libs.spring.cloud.starter.consul.discovery)
    implementation(libs.spring.cloud.starter.loadbalancer)
    implementation(libs.spring.boot.starter.oauth2.resource.server)

    // import JUnit BOM
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
