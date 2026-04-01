
plugins {
    id("org.springframework.boot")
}

dependencies {
    // Spring boot
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.spring.cloud.bom))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.cloud.starter.consul.discovery)
    implementation(libs.spring.authorization.server)
    implementation(libs.spring.boot.starter.oauth2.client)
    developmentOnly(libs.spring.boot.devtools)


    // import JUnit BOM
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
