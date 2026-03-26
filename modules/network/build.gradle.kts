plugins {
    kotlin("jvm")
}

group = "com.jason"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    api("com.squareup.okhttp3:okhttp:4.12.0")
    api("com.jakewharton:disklrucache:2.0.2")
    implementation("org.json:json:20220320")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

kotlin {
    jvmToolchain(8)
}

tasks.test {
    useJUnitPlatform()
}