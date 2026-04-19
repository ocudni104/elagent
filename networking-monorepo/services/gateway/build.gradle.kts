import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("java")
    alias(libs.plugins.spring.boot)
}

group = "ocudni104"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val appMainClass = "ocudni104.gateway.GatewayApplication"

springBoot {
    mainClass.set(appMainClass)
}

tasks.named<BootRun>("bootRun") {
    mainClass.set(appMainClass)
    systemProperty(
        "spring.profiles.active",
        System.getProperty("spring.profiles.active") ?: "local"
    )
}

tasks.register<BootRun>("bootRunInContainer") {
    group = "application"
    description = "Run the app with the container ready profile"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(appMainClass)
    systemProperty(
        "spring.profiles.active",
        System.getProperty("spring.profiles.active") ?: "container"
    )
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    // Spring boot
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.jdbc)
    runtimeOnly(libs.postgresql)
    testImplementation(libs.spring.boot.starter.test)

    implementation(platform(libs.spring.cloud.bom))
    implementation(libs.spring.cloud.starter.gateway)
    implementation(libs.spring.cloud.starter.consul.discovery)
    implementation(libs.spring.cloud.starter.loadbalancer)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // import JUnit BOM
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
