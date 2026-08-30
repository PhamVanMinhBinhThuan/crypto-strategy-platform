plugins {
    id("crypto.java-library-conventions")
}

dependencies {
    api(project(":modules:domain"))
    implementation(project(":modules:market-data"))
    implementation(project(":modules:strategy-core"))
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)
}
