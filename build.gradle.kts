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
    base
}

allprojects {
    group = "io.github.f2bb.amalgamation"
    version = "1.0.0-SNAPSHOT"
}

// Projects to configure standard publishing
subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    extensions.getByType<JavaPluginExtension>().apply {
        withJavadocJar()
        withSourcesJar()

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.withType<AbstractArchiveTask> {
        from(rootProject.file("LICENSE"))
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"

        if (JavaVersion.current().isJava9Compatible) {
            options.release.set(8)
        } else {
            sourceCompatibility = "8"
            targetCompatibility = "8"
        }
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
        }

        repositories {
            maven {
                val releasesRepoUrl = uri("${rootProject.buildDir}/repos/releases")
                val snapshotsRepoUrl = uri("${rootProject.buildDir}/repos/snapshots")
                name = "Project"
                url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            }
        }
    }
}
