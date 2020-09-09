import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.4.0"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "de.alxgrk"
version = "1.0.1"

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

kotlin.target {
    compilations.all {
        kotlinOptions.jvmTarget = "1.8"
    }
}

val shadowJar = tasks.withType<Jar> {
    archiveBaseName.set("${project.name}-fat")
    archiveClassifier.set("")
    archiveVersion.set("")
    if (this.name == "shadowJar") {
        (this as ShadowJar).configurations =
            listOf(
                project.configurations.compileClasspath.get(),
                project.configurations.runtimeClasspath.get()
            )
    }
}
tasks["build"].dependsOn(shadowJar)

repositories {
    mavenLocal()
    jcenter()
    maven {
        setUrl("https://jitpack.io")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-io-jvm:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.ktor:ktor-metrics:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("com.github.jillesvangurp:es-kotlin-wrapper-client:1.0-X-Beta-9-7.9.0")
    implementation("com.mercateo:ktor-server-lambda-core:1.0.1")

    implementation("software.amazon.awssdk:auth:2.14.10")

    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")
