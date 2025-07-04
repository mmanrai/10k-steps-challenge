dependencies {
    implementation(libs.vertx.web)
    implementation(libs.vertx.web.client)
    implementation(libs.vertx.auth.jwt)
    implementation(libs.log4j.api)
    runtimeOnly(libs.log4j.core)
}

