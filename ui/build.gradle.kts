plugins {
    `java-library`
}

val versionSlf4j: String by rootProject.extra
val versionLemur: String by rootProject.extra
val versionJme: String by rootProject.extra

dependencies {
    api(project(":engine"))
    api(project(":shared"))

    implementation("com.simsilica:lemur:$versionLemur")
    implementation("org.jmonkeyengine:jme3-plugins:$versionJme")
    implementation("org.slf4j:slf4j-api:$versionSlf4j")
}
