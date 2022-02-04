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
import java.net.URI

plugins {
    base
    `java-gradle-plugin`
    `maven-publish`
    signing
}

group = "io.github.astrarre.amalgamation"
version = "1.0.1.1"

extensions.getByType<JavaPluginExtension>().apply {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

java {
    withSourcesJar()
}

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        name = "HalfOf2"
        url = uri("https://storage.googleapis.com/devan-maven/")
    }
    maven {
        name = "MinecraftForge"
        url = uri("https://files.minecraftforge.net/maven")
    }
    maven {
        url = uri("https://maven.hydos.cf/releases")
    }
    /*maven {
        url = uri("https://jitpack.io")
    }*/
}

dependencies {
    compileOnly("org.jetbrains", "annotations", "20.1.0")
    implementation(gradleApi())
    implementation("com.google.guava", "guava", "30.1-jre")
    implementation("com.google.code.gson", "gson", "2.8.6")
    implementation("org.ow2.asm", "asm-tree", "9.1")
    implementation("net.fabricmc:mercury:0.2.4")
    implementation("io.github.astrarre", "tiny-remapper", "1.0.3")
    implementation("it.unimi.dsi:fastutil:8.5.6")
    implementation("org.ow2.asm:asm-commons:9.1")
    implementation("net.fabricmc:mapping-io:0.2.1")
    implementation("net.fabricmc:access-widener-javaparser:3.0.0")
    implementation("io.github.coolmineman:trieharder:0.1.2")
    implementation("net.devtech:zip-io:3.2.6")
    implementation("com.google.jimfs:jimfs:1.2")
    compileOnly("net.fabricmc:fabric-fernflower:1.4.1")
    implementation("net.fabricmc.unpick:unpick:2.2.0")
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

tasks.withType<AbstractArchiveTask> {
    from(rootProject.file("LICENSE"))
}

tasks.withType<Javadoc> {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

extensions.getByType<PublishingExtension>().apply {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            extensions.getByType<SigningExtension>().apply {
                if (signatory != null) {
                    sign(this@create)
                }
            }
        }
        create<MavenPublication>("mavenTrolling") {


        }
    }

    repositories {
        /*maven {
            val releasesRepoUrl = uri("${rootProject.buildDir}/repos/releases")
            val snapshotsRepoUrl = uri("${rootProject.buildDir}/repos/snapshots")
            name = "Project"
            url = if (version.toString()
                    .endsWith("SNAPSHOT")
            ) snapshotsRepoUrl else releasesRepoUrl
        }*/
        maven {
            val mavenUrl = if(project.hasProperty("maven_url")) project.property("maven_url") else ""
            url = URI(mavenUrl as String)
            if (mavenUrl.startsWith("http")) {
                credentials {
                    username = if(project.hasProperty ("maven_username")) project.property("maven_username") as String else ""
                    password = if(project.hasProperty ("maven_password")) project.property("maven_password") as String else ""
                }
            }
        }
    }
}
