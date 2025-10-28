plugins {
    `java-library`
}

val versionSlf4j: String by rootProject.extra

dependencies {
    api(project(":shared"))

    implementation("org.slf4j:slf4j-api:$versionSlf4j")
}
