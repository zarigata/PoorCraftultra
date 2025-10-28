plugins {
    `java-library`
}

val versionSlf4j: String by rootProject.extra
val versionJackson: String by rootProject.extra

dependencies {
    api("org.slf4j:slf4j-api:$versionSlf4j")
    api("com.fasterxml.jackson.core:jackson-databind:$versionJackson")
}
