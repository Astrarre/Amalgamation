/*
 * Amalgamation
 * Copyright (C) 2021 Astrarre
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    compileOnly("org.jetbrains", "annotations", "20.1.0")
    implementation(project(":utils"))
    implementation("org.slf4j", "slf4j-api", "1.7.30")
    implementation("com.google.code.gson", "gson", "2.8.6")
    implementation("com.google.guava", "guava", "30.1-jre")
    implementation("net.fabricmc:tiny-mappings-parser:0.3.0+build.17")
    implementation ("org.ow2.asm:asm:9.1")
}

val org = "net.fabricmc"
val artifact = "yarn"
val minecraftVersion = "1.16.5"
val yarnBuild = "1.16.5+build.4"

fun get(): File {
    val config = project.configurations.detachedConfiguration()
    config.dependencies.add(project.dependencies.create("$org:$artifact:$yarnBuild:mergedv2"))
    return config.resolve().first()
}

val flatten = tasks.register<JavaExec>("flatten") {
    group = "run"
    classpath = sourceSets.main.get().runtimeClasspath
    main = "io.github.astrarre.amalgamation.mappings_flattener.Flattener"
    args(get().absolutePath, File(project.buildDir, "flattener/mappings/flattened.tiny").absolutePath, "1.16.5")
    workingDir("$rootDir/run")
}

val flattened = tasks.register<Jar>("flattened") {
    dependsOn(flatten)
    archiveBaseName.set(artifact)
    archiveVersion.set(yarnBuild)
    archiveClassifier.set("flattened")
    from(File(project.buildDir, "flattener"))
}

publishing {
    publications {
        create<MavenPublication>("flattenedYarn") {
            artifactId = "yarn-flattened"
            version = yarnBuild
            artifact(flattened) {
                builtBy(flattened)
            }
        }
    }

    repositories {
        maven {
            val mavenUrl = if(project.hasProperty("maven_url")) project.property("maven_url") as String else ""
            url = uri(mavenUrl)
            if (mavenUrl.startsWith("http")) {
                credentials {
                    username = if(project.hasProperty("maven_username")) project.property("maven_username") as String else ""
                    password = if(project.hasProperty("maven_password")) project.property("maven_password") as String else ""
                }
            }
        }
    }
}
