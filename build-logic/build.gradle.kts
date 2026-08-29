plugins {
    `kotlin-dsl`
}

group = "com.cryptostrategy.platform.buildlogic"

dependencies {
    implementation("org.springframework.boot:spring-boot-gradle-plugin:3.5.16")

    testImplementation(gradleTestKit())
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
