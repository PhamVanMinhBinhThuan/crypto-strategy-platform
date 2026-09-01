plugins {
    id("crypto.java-library-conventions")
}

dependencies {
    api(project(":modules:domain"))
    implementation(project(":modules:market-data"))
    api(project(":modules:strategy-core"))
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.jackson.databind)
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation(libs.slf4j.api)
    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation("org.assertj:assertj-core")
}
