package io.github.astrarre.amalgamation.gradle.dependencies.remap.source;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.AmalgamationRemapper;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import io.github.coolmineman.trieharder.FindReplaceSourceRemapper;
import it.unimi.dsi.fastutil.Pair;
import net.devtech.filepipeline.api.VirtualFile;
import net.devtech.filepipeline.api.source.VirtualSink;
import net.devtech.filepipeline.api.source.VirtualSource;
import org.objectweb.asm.commons.Remapper;

public class TrieHarderRemapperImpl implements AmalgamationRemapper {
	FindReplaceSourceRemapper sourceRemapper;
	
	@Override
	public void acceptRemaps(List<Pair<Artifact, Artifact>> fromTos) throws Exception {
		for(Pair<Artifact, Artifact> to : fromTos) {
			VirtualSource source = to.left().file.openAsSource();
			VirtualSink sink = AmalgIO.DISK_OUT.subsink(to.right().file);
			source.depthStream().filter(VirtualFile.class::isInstance).filter(p -> p.relativePath().endsWith(".java")).forEach(v -> {
				String path = v.relativePath();
				String contents = ((VirtualFile) v).asString(StandardCharsets.UTF_8);
				String remappedContents = this.sourceRemapper.remapString(contents);
				String internalName = path.substring(0, path.length() - 5);
				String mappedInternalName = this.sourceRemapper.remapString(internalName.replace('/', '.')).replace('.', '/');
				VirtualFile output = sink.outputFile(mappedInternalName + ".java");
				sink.writeString(output, remappedContents, StandardCharsets.UTF_8);
			});
		}
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
