plugins {
    `java-library`
}

group = "dev.lewai"
version = "0.2.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven {
        name = "hytale-release"
        url = uri("https://maven.hytale.com/release")
    }
    maven {
        name = "hytale-pre-release"
        url = uri("https://maven.hytale.com/pre-release")
    }
}

dependencies {
    compileOnly("com.hypixel.hytale:Server:2026.03.26-89796e57b")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.jar {
    archiveBaseName.set("PrefabOptimizer-Extended")
    archiveVersion.set(project.version.toString())
    manifest {
        attributes(
            "Implementation-Title" to "PrefabOptimizer-Extended",
            "Implementation-Version" to project.version.toString()
        )
    }
}

tasks.register<Copy>("install") {
    description = "Alias for build that matches the Maven 'install' muscle memory."
    group = "build"
    dependsOn(tasks.jar)
    from(tasks.jar)
    into(layout.buildDirectory.dir("libs"))
}
