plugins {
    `java-library`
}

val versionSlf4j: String by rootProject.extra
val versionKryo: String by rootProject.extra
val versionJme: String by rootProject.extra

dependencies {
    api(project(":shared"))

    implementation("org.jmonkeyengine:jme3-networking:$versionJme")
    implementation("com.esotericsoftware:kryo:$versionKryo")
    implementation("org.slf4j:slf4j-api:$versionSlf4j")
}
