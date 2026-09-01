plugins {
    id("crypto.java-library-conventions")
}

sourceSets.main {
    resources.setSrcDirs(listOf("src/main/resources"))
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.jackson.databind)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jackson.databind)
}
