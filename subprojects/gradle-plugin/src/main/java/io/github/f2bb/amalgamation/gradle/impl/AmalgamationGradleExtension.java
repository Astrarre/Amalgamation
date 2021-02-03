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

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import groovy.lang.Closure;
import io.github.f2bb.amalgamation.gradle.dependencies.CachedSelfResolvingDependency;
import io.github.f2bb.amalgamation.gradle.base.GenericPlatformSpec;
import io.github.f2bb.amalgamation.gradle.dependencies.MinecraftDependency;
import io.github.f2bb.amalgamation.gradle.extensions.LauncherMeta;
import io.github.f2bb.amalgamation.gradle.impl.cache.Cache;
import io.github.f2bb.amalgamation.gradle.minecraft.MappingTarget;
import io.github.f2bb.amalgamation.gradle.minecraft.MinecraftAmalgamation;
import io.github.f2bb.amalgamation.gradle.minecraft.MinecraftAmalgamationGradlePlugin;
import io.github.f2bb.amalgamation.gradle.minecraft.MinecraftPlatformSpec;
import org.cadixdev.lorenz.MappingSet;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileCollection;
import org.gradle.util.ConfigureUtil;

public class AmalgamationGradleExtension implements MinecraftAmalgamation {

	private final Project project;
	private final Cache globalCache;
	private final LauncherMeta meta;

	private final Set<GenericPlatformSpec> genericSpecs = new HashSet<>();
	private final Set<Forge> forgeSpecs = new HashSet<>();
	private final Set<Fabric> fabricSpecs = new HashSet<>();
	private final Configuration mappings;

	private Dependency myDependency;

	public AmalgamationGradleExtension(Project project) {
		this.project = project;
		this.mappings = project.getConfigurations().detachedConfiguration();
		this.globalCache = Cache.globalCache(project);
		this.meta = MinecraftAmalgamationGradlePlugin.getLauncherMeta(project);
	}

	@Override
	public Dependency client(String version) {
		return new MinecraftDependency(this.project, this.meta, version, this.globalCache, true);
	}

	@Override
	public Dependency server(String version) {
		return new MinecraftDependency(this.project, this.meta, version, this.globalCache, false);
	}

	@Override
	public void mappings(Object dependencyNotation) {
		this.mappings.getDependencies().add(this.project.getDependencies().create(dependencyNotation));
	}

	@Override
	public void forge(String minecraftVersion, Object dependency, Closure configureClosure) {
		this.forgeAction(minecraftVersion, dependency, spec -> {
			ConfigureUtil.configure(configureClosure, spec);
		});
	}

	@Override
	public void forgeAction(String minecraftVersion, Object dependency, Action<MinecraftPlatformSpec> configureAction) {
		this.assertMutable();

		MinecraftPlatformSpec forge = new MinecraftPlatformSpec(this.project);
		forge.name("forge");
		forge.name(minecraftVersion);
		configureAction.execute(forge);

		this.forgeSpecs.add(new Forge(this.project, minecraftVersion, this.project.getDependencies().create(dependency), forge));
	}

	@Override
	public void fabric(String minecraftVersion, Closure configureClosure) {
		this.fabricAction(minecraftVersion, spec -> {
			ConfigureUtil.configure(configureClosure, spec);
		});
	}

	@Override
	public void fabricAction(String minecraftVersion, Action<MinecraftPlatformSpec> configureAction) {
		this.assertMutable();

		MinecraftPlatformSpec fabric = new MinecraftPlatformSpec(this.project);
		fabric.name("fabric");
		fabric.name(minecraftVersion);
		configureAction.execute(fabric);

		this.fabricSpecs.add(new Fabric(this.project, minecraftVersion, fabric));
	}

	@Override
	public MappingSet createMappings(MappingTarget target, String version) {
		return MappingSet.create();
	}

	@Override
	public FileCollection getMappedClasspath(Collection<String> platforms) {
		return this.getClasspath(platforms);
	}

	protected void assertMutable() {
		if (this.myDependency != null) {
			throw new IllegalStateException("Dependency matrix is frozen");
		}
	}

	@Override
	public void generic(Closure configureClosure) {
		this.genericAction(spec -> {
			ConfigureUtil.configure(configureClosure, spec);
		});
	}

	@Override
	public void genericAction(Action<GenericPlatformSpec> configureAction) {
		this.assertMutable();

		GenericPlatformSpec spec = new GenericPlatformSpec(this.project);
		configureAction.execute(spec);

		if (spec.getNames().isEmpty()) {
			throw new IllegalStateException("No names were given to this platform");
		}

		if (spec.getDependencies().isEmpty()) {
			throw new IllegalStateException("No dependencies were given to this platform");
		}

		this.genericSpecs.add(spec);
	}

	@Override
	public Dependency create() throws IOException {
		if (this.myDependency != null) {
			return this.myDependency;
		}

		return this.myDependency = AmalgamationImpl.createDependencyFromMatrix(this.project, this.mappings, this.forgeSpecs, this.fabricSpecs, this.genericSpecs);
	}

	@Override
	public FileCollection getClasspath(Collection<String> platforms) {
		Configuration classpath = this.project.getConfigurations().detachedConfiguration();

		for (Forge spec : this.forgeSpecs) {
			if (spec.forge.getNames().containsAll(platforms)) {
				classpath.extendsFrom(spec.forge.getDependencies());
				classpath.extendsFrom(spec.forge.getRemap());
			}
		}

		for (Fabric spec : this.fabricSpecs) {
			if (spec.fabric.getNames().containsAll(platforms)) {
				classpath.extendsFrom(spec.fabric.getDependencies());
				classpath.extendsFrom(spec.fabric.getRemap());
			}
		}

		for (GenericPlatformSpec spec : this.genericSpecs) {
			if (spec.getNames().containsAll(platforms)) {
				classpath.extendsFrom(spec.getDependencies());
			}
		}

		return classpath.getAsFileTree();
	}
}
