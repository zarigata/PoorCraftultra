plugins {
    `java-library`
}

val versionSlf4j: String by rootProject.extra
val versionOpenSimplex2: String by rootProject.extra
val versionZstdJni: String by rootProject.extra

dependencies {
    api(project(":voxel"))
    api(project(":shared"))

    implementation("com.github.KdotJPG:OpenSimplex2:$versionOpenSimplex2")
    implementation("com.github.luben:zstd-jni:$versionZstdJni")
    implementation("org.slf4j:slf4j-api:$versionSlf4j")
}
