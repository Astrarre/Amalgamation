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
    implementation(gradleApi())
    implementation("com.google.code.gson", "gson", "2.8.6")
    implementation("com.google.guava", "guava", "30.1-jre")
    implementation("net.fabricmc:tiny-mappings-parser:0.3.0+build.17")
    implementation("org.ow2.asm:asm-commons:9.1")
    implementation("org.ow2.asm:asm:9.1")
}

val org = "net.fabricmc"
val artifact = "yarn"
val minecraftVersion = "1.16.5"
val yarnBuild = "1.16.5+build.4"

fun get(ext: String): File {
    val config = project.configurations.detachedConfiguration()
    config.dependencies.add(project.dependencies.create("$org:$artifact:$yarnBuild$ext"))
    return config.resolve().first()
}

val flatten = tasks.register<JavaExec>("flatten") {
    group = "run"
    classpath = sourceSets.main.get().runtimeClasspath
    main = "io.github.astrarre.amalgamation.mappings_flattener.Flattener"
    args(get(":v2").absolutePath, File(project.buildDir, "flattener/v2/mappings/mappings.tiny").absolutePath, "1.16.5")
    workingDir("$rootDir/run")
}

val flattened = tasks.register<Jar>("flattened") {
    dependsOn(flatten)
    archiveBaseName.set(artifact)
    archiveVersion.set(yarnBuild)
    archiveClassifier.set("v2")
    from(File(project.buildDir, "flattener/v2"))
}


val flattenMerged = tasks.register<JavaExec>("flattenMerged") {
    group = "run"
    classpath = sourceSets.main.get().runtimeClasspath
    main = "io.github.astrarre.amalgamation.mappings_flattener.Flattener"
    args(get(":mergedv2").absolutePath, File(project.buildDir, "flattener/mergedv2/mappings/mappings.tiny").absolutePath, "1.16.5")
    workingDir("$rootDir/run")
}

val flattenedMerged = tasks.register<Jar>("flattenedMerged") {
    dependsOn(flattenMerged)
    archiveBaseName.set(artifact)
    archiveVersion.set(yarnBuild)
    archiveClassifier.set("mergedv2")
    from(File(project.buildDir, "flattener/mergedv2"))
}

publishing {
    publications {
        create<MavenPublication>("flattenedYarn") {
            artifactId = "yarn-flattened"
            version = yarnBuild.replace('+', '-')
            artifact(flattenedMerged) {
                builtBy(flattenedMerged)
            }
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
