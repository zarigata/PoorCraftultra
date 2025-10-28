plugins {
    `java-library`
}

val versionSlf4j: String by rootProject.extra
val versionJackson: String by rootProject.extra

dependencies {
    api(project(":voxel"))
    api(project(":shared"))

    implementation("com.fasterxml.jackson.core:jackson-databind:$versionJackson")
    implementation("org.slf4j:slf4j-api:$versionSlf4j")
}
