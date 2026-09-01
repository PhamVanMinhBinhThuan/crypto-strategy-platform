plugins {
    id("crypto.java-library-conventions")
}

sourceSets.main {
    resources.setSrcDirs(listOf("src/main/resources"))
}

dependencies {
    api(project(":modules:domain"))
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.jackson.databind)
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation("org.assertj:assertj-core")
    testImplementation(libs.jackson.databind)
}
