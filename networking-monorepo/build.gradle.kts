plugins {
    id("java")
    alias(libs.plugins.spring.boot) version libs.versions.springboot apply false

}

allprojects {
    group = "com.example"
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}