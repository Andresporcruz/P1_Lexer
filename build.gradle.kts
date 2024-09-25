plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    // JUnit 5 dependencies for testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")

    // Parameterized tests (JUnitParams)
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.0")
}


tasks.test {
    useJUnitPlatform()
}

