/*
 * Copyright (c) 2016, 2018, Player, asie
 * Copyright (c) 2021, FabricMC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.astrarre.amalgamation.gradle.dependencies.remap.misc;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.AmalgamationRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.binary.TinyRemapperImpl;
import io.github.astrarre.amalgamation.gradle.tasks.remap.remap.AwResourceRemapper;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import it.unimi.dsi.fastutil.Pair;
import org.objectweb.asm.commons.Remapper;

import net.fabricmc.tinyremapper.MetaInfFixer;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;

public class ResourceRemapper implements AmalgamationRemapper {
	TinyRemapper remapper;
	final ResourceRemapperProvider rssRemapper;
	public final String uniqueString;
	
	public interface ResourceRemapperProvider {
		default void acceptMappings(
				List<Mappings.Namespaced> mappings, Remapper remapper) {}
		
		OutputConsumerPath.ResourceRemapper createRemapper();
	}
	
	public static ResourceRemapper metaInfFixer() {
		return new ResourceRemapper(() -> MetaInfFixer.INSTANCE, "meta_inf_fixer");
	}
	
	public static ResourceRemapper accessWidener() {
		return new ResourceRemapper(new ResourceRemapperProvider() {
			String mappings;
			
			@Override
			public OutputConsumerPath.ResourceRemapper createRemapper() {
				return new AwResourceRemapper(() -> this.mappings);
			}
			
			@Override
			public void acceptMappings(List<Mappings.Namespaced> mappings, Remapper remapper) {
				this.mappings = mappings.get(0).to();
			}
		}, "meta_inf_fixer");
	}
	
	public ResourceRemapper(ResourceRemapperProvider remapper, String string) {
		this.rssRemapper = remapper;
		this.uniqueString = string;
	}
	
	@Override
	public void hash(Hasher hasher) {
		hasher.putString(this.uniqueString, StandardCharsets.UTF_8);
	}
	
	@Override
	public void acceptRemap(List<AmalgamationRemapper> remappers, List<Pair<Artifact, Artifact>> fromTos) {
		for(AmalgamationRemapper amalgamationRemapper : remappers) {
			if(amalgamationRemapper instanceof TinyRemapperImpl impl) {
				this.remapper = impl.remapper;
			}
		}
		Objects.requireNonNull(this.remapper, "ResourceRemapper " + this.uniqueString + " requires tiny remapper remapper!");
	}
	
	@Override
	public RemapTask createTask(RemapTarget target) {
		OutputConsumerPath.ResourceRemapper remapper = this.rssRemapper.createRemapper();
		return (srcFs, srcPath, dstFs, handled) -> {
			if(remapper.canTransform(this.remapper, srcPath)) { // todo maybe optimize this a bit
				try(InputStream is = Files.newInputStream(srcPath)) {
					remapper.transform(dstFs.getPath("/"), srcPath, is, this.remapper);
				}
				return true;
			}
			return false;
		};
	}
	
	@Override
	public void acceptMappings(
			List<Mappings.Namespaced> mappings, Remapper remapper) {
		this.rssRemapper.acceptMappings(mappings, remapper);
	}
}
