plugins {
    id("crypto.java-library-conventions")
}

dependencies {
    api(project(":modules:domain"))
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)
}
