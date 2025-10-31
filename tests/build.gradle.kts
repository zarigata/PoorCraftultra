dependencies {
    testImplementation(project(":shared"))
    testImplementation(project(":engine-api"))
    testImplementation(project(":app"))
    testImplementation(project(":voxel"))
    testImplementation(project(":world"))
    testImplementation("org.junit.jupiter:junit-jupiter:${rootProject.extra["junitVersion"]}")
    testImplementation("org.mockito:mockito-core:${rootProject.extra["mockitoVersion"]}")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:${rootProject.extra["jacksonVersion"]}")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    testLogging {
        showStandardStreams = true
        events("passed", "skipped", "failed")
    }
    reports.html.required.set(true)
    reports.html.outputLocation.set(layout.buildDirectory.dir("reports/tests"))
}
