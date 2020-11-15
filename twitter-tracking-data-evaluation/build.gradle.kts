import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.10"
    application
}
group = "de.alxgrk"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
    maven {
        setUrl("https://jitpack.io")
    }
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")
    implementation("com.github.ajalt.clikt:clikt:3.0.1")
    implementation("com.github.jillesvangurp:es-kotlin-wrapper-client:1.0-X-Beta-9-7.9.0")
    implementation("software.amazon.awssdk:auth:2.14.10")
    implementation("org.nield:kotlin-statistics:1.2.1")
    implementation("kscience.plotlykt:plotlykt-core:0.2.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.3")
    implementation("de.undercouch:bson4jackson:2.11.0")
    implementation("org.reflections:reflections:0.9.12")
    implementation("org.testcontainers:testcontainers:1.15.0")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
    testImplementation("io.mockk:mockk:1.10.2")
}
tasks.withType<KotlinCompile>().all {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf("-Xinline-classes")
    }
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClassName = "de.alxgrk.data.MainKt"
}