package io.github.astrarre.amalgamation.gradle.dependencies.remap.source;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.AmalgamationRemapper;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import io.github.coolmineman.trieharder.FindReplaceSourceRemapper;
import org.objectweb.asm.commons.Remapper;

public class TrieHarderRemapperImpl implements AmalgamationRemapper {
	FindReplaceSourceRemapper sourceRemapper;
	
	@Override
	public RemapTask createTask(RemapTarget target) {
		return (srcFs, srcPath, dstFs, handled) -> {
			String path = srcPath.toString();
			if(path.endsWith(".class")) {
				String contents = Files.readString(srcPath);
				String remappedContents = this.sourceRemapper.remapString(contents);
				String internalName = path.substring(0, path.length() - 5);
				String mappedInternalName = this.sourceRemapper.remapString(internalName.replace('/', '.')).replace('.', '/');
				Path output = dstFs.getPath(mappedInternalName + ".java");
				Files.writeString(output, remappedContents);
				return true;
			} else {
				return false;
			}
		};
	}
	
	@Override
	public void acceptMappings(List<Mappings.Namespaced> list, Remapper remapper) {
		Mappings.Namespaced namespaced = Iterables.getOnlyElement(list);
		this.sourceRemapper = new FindReplaceSourceRemapper(namespaced.tree(), namespaced.fromI(), namespaced.toI());
	}

	@Override
	public void hash(Hasher hasher) {
		hasher.putString("trieharder", StandardCharsets.UTF_8);
	}
}
