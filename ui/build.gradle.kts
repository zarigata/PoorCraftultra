dependencies {
    implementation(project(":shared"))
    implementation(project(":engine"))
    implementation("com.simsilica:lemur:${rootProject.extra["lemurVersion"]}")
    implementation("com.simsilica:lemur-proto:1.13.0")
}
