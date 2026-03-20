plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    // Spring boot
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.starter.actuator)
    testImplementation(libs.spring.boot.starter.test)

    implementation(platform(libs.spring.cloud.bom))
    implementation(libs.spring.cloud.starter.gateway)
    implementation(libs.spring.boot.starter.oauth2.resource.server)

    // import JUnit BOM
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}