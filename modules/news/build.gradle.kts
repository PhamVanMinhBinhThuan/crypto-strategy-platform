plugins {
    id("crypto.java-library-conventions")
}

dependencies {
    api(project(":modules:domain"))
    implementation(libs.jsoup)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}
