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
}

val minecraft_version: String by project
val forge_version: String by project

tasks.processResources {
    inputs.properties("forge_version" to forge_version, "minecraft_version" to minecraft_version)

    filesMatching("gradle_data.properties") {
        expand("forge_version" to forge_version, "minecraft_version" to minecraft_version)
    }
}

repositories {
    mavenCentral()
    maven {
        name = "MinecraftForge"
        url = uri("https://files.minecraftforge.net/maven")
    }

    maven {
        name = "HalfOf2"
        url = uri("https://storage.googleapis.com/devan-maven/")
    }
}

dependencies {
    api("org.ow2.asm:asm:9.0")
    api("org.ow2.asm:asm-tree:9.0")

    implementation(project(":api"))
    implementation("org.ow2.asm:asm-commons:9.0")
    implementation("net.minecraftforge:forge:$minecraft_version-$forge_version:installer")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("io.github.java-diff-utils:java-diff-utils:4.5")
    implementation("net.devtech:signutil:1.0.0")
}

tasks.register<Jar>("testJar") {
    classifier = "tests"
    from(sourceSets.test.get().output)
}

tasks.register<JavaExec>("testDiff") {
    group = "tests"
    description = "Test Diff:tm:"
    classpath = sourceSets.test.get().runtimeClasspath
    main = "io.github.astrarre.merger.test.MergeTest"
    args(tasks.named("testJar").get().outputs.files.first())
    workingDir("$rootDir/run")
    dependsOn(tasks.named("testJar"))
}