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
    `java-gradle-plugin`
}

extensions.getByType<JavaPluginExtension>().apply {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

java {
    withSourcesJar()
}

val forge_version: String by project

dependencies {
    compileOnly("org.jetbrains", "annotations", "20.1.0")
    implementation(gradleApi())

    implementation(rootProject.project(":api"))
    implementation("com.google.guava", "guava", "30.1-jre")
    //implementation("net.devtech", "signutil", "1.0.0")
    implementation("com.google.code.gson", "gson", "2.8.6")
    implementation("org.ow2.asm", "asm-tree", "9.1")
    implementation("net.fabricmc:mercury:0.2.4")
    implementation("io.github.astrarre", "tiny-remapper", "1.0.1")
    implementation("it.unimi.dsi:fastutil:8.5.6")
    implementation("org.ow2.asm:asm-commons:9.1")
    implementation("net.fabricmc:mapping-io:0.2.1")
    implementation("net.fabricmc:access-widener-javaparser:3.0.0") {
        exclude("javaparser-symbol-solver-core")
    }
    implementation("io.github.coolmineman:trieharder:0.1.2")
    implementation("net.devtech:zip-io:3.2.1")
    compileOnly("org.jetbrains.gradle.plugin.idea-ext:org.jetbrains.gradle.plugin.idea-ext.gradle.plugin:1.1")
    implementation("com.google.jimfs:jimfs:1.2")
    //implementation("com.github.javaparser:javaparser-symbol-solver-core:3.23.1")
    implementation("net.minecraftforge:forge:1.17.1-37.0.75:installer")
    implementation("net.fabricmc:fabric-fernflower:1.4.1")
}

gradlePlugin {
    plugins {
        create("base") {
            id = "amalgamation-base"
            implementationClass = "io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationGradlePlugin"
        }

        create("minecraft") {
            id = "amalgamation-minecraft"
            implementationClass = "io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin"
        }
    }
}
