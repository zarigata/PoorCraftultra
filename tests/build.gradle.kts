plugins {
    `java-library`
}

val versionSlf4j: String by rootProject.extra
val versionJunit: String by rootProject.extra
val versionMockito: String by rootProject.extra
val versionAssertJ: String by rootProject.extra

dependencies {
    api(project(":app"))
    api(project(":engine"))
    api(project(":voxel"))
    api(project(":world"))
    api(project(":player"))
    api(project(":gameplay"))
    api(project(":net"))
    api(project(":steam"))
    api(project(":discord"))
    api(project(":mods"))
    api(project(":ai"))
    api(project(":ui"))
    api(project(":tools"))
    api(project(":shared"))

    testImplementation("org.junit.jupiter:junit-jupiter:$versionJunit")
    testImplementation("org.mockito:mockito-core:$versionMockito")
    testImplementation("org.assertj:assertj-core:$versionAssertJ")
    testImplementation("org.slf4j:slf4j-api:$versionSlf4j")
}
