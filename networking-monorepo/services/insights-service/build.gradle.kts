
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("java")
    id("maven-publish")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spotless)
}

group = "ocudni104"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val appMainClass = "ocudni104.insights.InsightsServiceApplication"

springBoot {
    mainClass.set(appMainClass)
}

tasks.named<BootRun>("bootRun") {
    mainClass.set(appMainClass)
    systemProperty(
        "spring.profiles.active",
        System.getProperty("spring.profiles.active") ?: "local",
    )
}

tasks.register<BootRun>("bootRunInContainer") {
    group = "application"
    description = "Run the app with the container ready profile"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(appMainClass)
    systemProperty(
        "spring.profiles.active",
        System.getProperty("spring.profiles.active") ?: "container",
    )
}

tasks.withType<Test> {
    useJUnitPlatform()
}

spotless {
    java {
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("misc") {
        target("*.md", "*.yml", "*.yaml", ".gitignore")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

dependencies {
    // Spring boot
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.spring.cloud.bom))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.jdbc)
    runtimeOnly(libs.postgresql)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.cloud.starter.consul.discovery)
    testImplementation(libs.spring.boot.starter.test)
    developmentOnly(libs.spring.boot.devtools)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // import JUnit BOM
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
