dependencies {
    implementation(project(":shared"))
    implementation(project(":engine-api"))
    implementation(project(":gameplay"))
    implementation("com.simsilica:lemur:${rootProject.extra["lemurVersion"]}")
    implementation("com.simsilica:lemur-proto:1.13.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:${rootProject.extra["jacksonVersion"]}")
}
