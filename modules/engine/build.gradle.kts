import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
//    id("maven-publish")
//    // 添加Shadow插件用于创建包含依赖的fat jar
//    id("com.github.johnrengelman.shadow") version "8.1.1" // 使用最新版本的Shadow插件
}

group = "com.jason"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    api("org.jsoup:jsoup:1.17.2")
    api("cn.wanghaomiao:JsoupXpath:2.5.1")
    api("com.jayway.jsonpath:json-path:2.9.0")
    api("com.google.code.gson:gson:2.8.9")
    implementation(project(":modules:network"))
    implementation("org.json:json:20251224")
}

kotlin {
    jvmToolchain(8)
}

tasks.test {
    useJUnitPlatform()
}
// 配置Shadow插件以创建包含所有依赖的fat jar
//tasks.jar {
//    enabled = false // 禁用默认的jar任务
//}
//
//// 创建一个新的shadowJar任务
//tasks.shadowJar {
//    archiveClassifier.set("") // 不添加classifier，直接覆盖默认的jar名称
//
//    // 合并策略 - 处理重复的文件
//    mergeServiceFiles()
//    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.EC")
//}
//
//val libVersion = "1.0.0"
//
//// 生成源代码 JAR
//val sourcesJar by tasks.registering(Jar::class) {
//    archiveClassifier.set("sources")
//    from(sourceSets.main.get().allSource)
//}
//
//// 生成 JavaDoc JAR
//val javadocJar by tasks.registering(Jar::class) {
//    dependsOn(tasks.javadoc)
//    archiveClassifier.set("javadoc")
//    from(tasks.javadoc.get().destinationDir)
//}
//
//tasks.withType<Jar> {
//    archiveBaseName.set("SearchEngine") // 设置构件的基本名称
//    archiveVersion.set(libVersion) // 设置构件的版本
//}
//
//tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
//    compilerOptions {
//        jvmTarget.set(JvmTarget.JVM_1_8) // 设置 JVM 目标版本
//    }
//}
//
//afterEvaluate {
//    publishing {
//        publications {
//            create<MavenPublication>("mavenJava") {
//                from(components["java"])
//                groupId = "com.jason"
//                artifactId = "search-engine"
//                version = libVersion
//
//                // 添加源代码和 JavaDoc 构件
//                artifact(sourcesJar.get())
//                artifact(javadocJar.get())
//
//                System.out.println("implementation(\"$groupId:$artifactId:$version\")")
//            }
//        }
//    }
//}