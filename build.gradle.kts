plugins {
    java
}

extra.apply {
    set("jmeVersion", "3.6.1-stable")
    set("lwjglVersion", "3.3.3")
    set("lemurVersion", "1.16.0")
    set("slf4jVersion", "2.0.9")
    set("logbackVersion", "1.4.11")
    set("typesafeConfigVersion", "1.4.3")
    set("junitVersion", "5.10.1")
    set("mockitoVersion", "5.7.0")
    set("jacksonVersion", "2.15.2")
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
        options.encoding = "UTF-8"
    }

    dependencies {
        "implementation"("org.slf4j:slf4j-api:${rootProject.extra["slf4jVersion"]}")
        "testImplementation"("org.junit.jupiter:junit-jupiter:${rootProject.extra["junitVersion"]}")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

tasks.named<Delete>("clean") {
    delete(rootProject.buildDir)
    subprojects.forEach { dependsOn(it.tasks.named("clean")) }
}
