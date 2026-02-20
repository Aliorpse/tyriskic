plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    id("com.gradleup.shadow") version "9.3.1"
}

group = "tech.aliorpse.tyriskic"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation("io.ktor:ktor-client-core:3.4.0")
    implementation("io.ktor:ktor-client-cio:3.4.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.4.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.0")

    implementation("org.ntqqrev:saltify-core:1.2.0-RC2")
}

kotlin {
    jvmToolchain(25)
}

tasks {
    shadowJar {
        manifest {
            attributes["Main-Class"] = "tech.aliorpse.tyriskic.TyriskicApplicationKt"
        }
    }
}
