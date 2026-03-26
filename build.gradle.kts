import java.nio.charset.Charset

plugins {
    kotlin("jvm") version "2.3.10"
}

group = "com.jason"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":modules:engine"))
    implementation(project(":modules:network"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.json:json:20251224")
}

kotlin {
    jvmToolchain(8)
}

tasks.test {
    useJUnitPlatform()
}

// 基于提供的代码模式，在jar任务完成后调用JarToDEX.bat 生成可供安卓端调用的dex Jar文件
gradle.taskGraph.whenReady {
    allTasks.forEach { task ->
        if (task.name == "jar") {
            task.doLast {
                val jarFile = tasks.jar.get().archiveFile.get().asFile
                if (jarFile.exists()) {
                    val batFile = rootProject.file("DexTools/JarToDEX.bat")
                    println(batFile)
                    if (batFile.exists()) {
                        // 使用cmd /c start来调用，避免路径中的空格问题
                        val command = listOf(
                            "cmd", "/c",
                            "start", "/wait", "cmd", "/c",
                            "\"\"${batFile.absolutePath}\" \"${jarFile.absolutePath}\"\""
                        )

                        val process = ProcessBuilder(command)
                            .directory(rootProject.projectDir)
                            .inheritIO()
                            .start()

                        // 读取标准输出（使用GBK解码）
                        process.inputStream.bufferedReader(Charset.forName("GBK")).use { reader ->
                            reader.forEachLine { line ->
                                println(line)
                            }
                        }

                        // 读取错误输出（使用GBK解码）
                        process.errorStream.bufferedReader(Charset.forName("GBK")).use { reader ->
                            reader.forEachLine { line ->
                                System.err.println(line)
                            }
                        }

                        val exitCode = process.waitFor()
                        if (exitCode != 0) {
                            throw GradleException("JarToDEX.bat execution failed with exit code $exitCode")
                        }
                        println("✓ Successfully converted ${jarFile.name} to DEX format")
                    } else {
                        throw GradleException("JarToDEX.bat not found at ${batFile.absolutePath}")
                    }
                }
            }
        }
    }
}