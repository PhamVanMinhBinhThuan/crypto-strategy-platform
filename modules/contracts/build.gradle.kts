plugins {
    id("crypto.java-library-conventions")
}

dependencies {
    api(project(":modules:domain"))
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.jackson.databind)
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
