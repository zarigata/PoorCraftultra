plugins {
    `java-library`
}

val versionJme: String by rootProject.extra
val versionSlf4j: String by rootProject.extra

dependencies {
    api(project(":engine"))
    api(project(":voxel"))
    api(project(":shared"))

    implementation("org.jmonkeyengine:jme3-bullet:$versionJme")
    implementation("org.jmonkeyengine:jme3-bullet-native:$versionJme")
    implementation("org.slf4j:slf4j-api:$versionSlf4j")
}
