package io.github.astrarre.amalgamation.gradle.dependencies.remap.source;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.AmalgRemapper;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.ZipRemapper;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import io.github.coolmineman.trieharder.FindReplaceSourceRemapper;
import org.objectweb.asm.commons.Remapper;

public class TrieHarderRemapperImpl implements AmalgRemapper {
	FindReplaceSourceRemapper sourceRemapper;

	@Override
	public boolean requiresClasspath() {
		return false;
	}

	@Override
	public boolean hasPostStage() {
		return false;
	}

	@Override
	public void acceptMappings(List<Mappings.Namespaced> list, Remapper remapper) {
		Mappings.Namespaced namespaced = Iterables.getOnlyElement(list);
		this.sourceRemapper = new FindReplaceSourceRemapper(namespaced.tree(), namespaced.fromI(), namespaced.toI());
	}

	@Override
	public ZipRemapper createNew() {
		return (entry, isClasspath) -> {
			String path = entry.path();
			if(path.endsWith(".java")) {
				String contents = StandardCharsets.UTF_8.decode(entry.read()).toString();
				String remappedContents = this.sourceRemapper.remapString(contents);
				String internalName = path.substring(0, path.length() - 5);
				String remappedFilePath = this.sourceRemapper.remapString(internalName.replace('/', '.')).replace('.', '/');
				entry.write(remappedFilePath + ".java", ByteBuffer.wrap(remappedContents.getBytes(StandardCharsets.UTF_8)));
				return true;
			}
			return false;
		};
	}

	@Override
	public void hash(Hasher hasher) {
		hasher.putString("trieharder", StandardCharsets.UTF_8);
	}
}
