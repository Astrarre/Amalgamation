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

repositories {
    mavenCentral()
    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net/")
    }
}

dependencies {
    compileOnly("org.jetbrains", "annotations", "20.1.0")
    implementation("org.slf4j", "slf4j-api", "1.7.30")
    implementation("com.google.code.gson", "gson", "2.8.6")
    implementation("com.google.guava", "guava", "30.1-jre")
    api("org.cadixdev", "lorenz", "0.5.6")
    api("net.fabricmc", "tiny-remapper", "0.3.2")
    api("net.fabricmc", "lorenz-tiny", "3.0.0")
}
