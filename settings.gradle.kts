plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "JarPluginDemo"
include(":modules:engine")
include(":modules:network")