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

repositories {
    mavenCentral()

    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net/")
    }

    maven {
        name = "MinecraftForge"
        url = uri("https://files.minecraftforge.net/maven")
    }

    maven {
        name = "HalfOf2"
        url = uri("https://storage.googleapis.com/devan-maven/")
    }

    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    compileOnly("org.jetbrains", "annotations", "20.1.0")

    implementation(project(":api"))
    implementation("com.google.guava", "guava", "30.1-jre")
    implementation("net.devtech", "signutil", "1.0.0")
    implementation("com.google.code.gson", "gson", "2.8.6")
    implementation("org.ow2.asm", "asm-tree", "9.1")
    implementation("org.cadixdev", "lorenz", "0.5.6")
    implementation("org.apache.commons", "commons-collections4", "4.4")
    implementation("net.fabricmc", "tiny-remapper", "0.3.2")
    implementation("net.fabricmc", "lorenz-tiny", "3.0.0")
    implementation("net.fabricmc", "dev-launch-injector", "0.2.1+build.8")
    implementation("com.github.javaparser:javaparser-core:3.22.0")
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.22.0")
}

gradlePlugin {
    plugins {
        create("base") {
            id = "io.github.astrarre.amalgamation.base"
            implementationClass = "io.github.astrarre.amalgamation.gradle.plugin.base.BaseAmalgamationGradlePlugin"
        }

        create("minecraft") {
            id = "io.github.astrarre.amalgamation.minecraft"
            implementationClass = "io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamationGradlePlugin"
        }
    }
}
