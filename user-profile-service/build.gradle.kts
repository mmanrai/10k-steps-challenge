plugins {
    java
}

dependencies {
    implementation(libs.vertx.web)
    implementation(libs.vertx.mongo.client)
    implementation(libs.vertx.auth.mongo)
    implementation(libs.log4j.api)
    runtimeOnly(libs.log4j.core)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.vertx.junit5)
    testImplementation(libs.rest.assured)
    testImplementation(libs.assertj.core)
    testImplementation(libs.testcontainers.junit.jupiter)

    testRuntimeOnly(libs.junit.jupiter.engine)
}