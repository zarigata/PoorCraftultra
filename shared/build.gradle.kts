plugins {
    `java-library`
}

dependencies {
    api("com.typesafe:config:${rootProject.extra["typesafeConfigVersion"]}")

    testImplementation("org.junit.jupiter:junit-jupiter:${rootProject.extra["junitVersion"]}")
    testImplementation("org.mockito:mockito-core:${rootProject.extra["mockitoVersion"]}")
}
