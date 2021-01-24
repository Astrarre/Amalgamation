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

package io.github.f2bb.amalgamation.gradle.impl;

import org.cadixdev.lorenz.MappingSet;

class MinecraftMappings {

    final MappingSet officialToIntermediary;
    final MappingSet intermediaryToNamed;
    final MappingSet officialToNamed;

    public MinecraftMappings(MappingSet officialToIntermediary, MappingSet intermediaryToNamed, MappingSet officialToNamed) {
        this.officialToIntermediary = officialToIntermediary;
        this.intermediaryToNamed = intermediaryToNamed;
        this.officialToNamed = officialToNamed;
    }
}
