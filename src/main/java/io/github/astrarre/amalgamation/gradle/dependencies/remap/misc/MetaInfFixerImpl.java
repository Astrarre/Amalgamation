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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.google.common.hash.Hasher;
import io.github.astrarre.amalgamation.gradle.dependencies.Artifact;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.AmalgamationRemapper;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Mappings;
import it.unimi.dsi.fastutil.Pair;
import net.devtech.filepipeline.api.VirtualFile;
import net.devtech.filepipeline.api.source.VirtualSink;
import net.devtech.filepipeline.api.source.VirtualSource;
import net.devtech.filepipeline.impl.util.FPInternal;
import org.objectweb.asm.commons.Remapper;

public class MetaInfFixerImpl implements AmalgamationRemapper {
	private static final String[] EMPTY = {};
	private Remapper remapper;
	
	@Override
	public void hash(Hasher hasher) {
		hasher.putString("META_INF fixer", StandardCharsets.UTF_8);
	}
	
	@Override
	public void acceptRemaps(List<Pair<Artifact, Artifact>> fromTos) throws Exception {
		for(Pair<Artifact, Artifact> to : fromTos) {
			VirtualSource source = to.left().file.openAsSource();
			VirtualSink sink = AmalgIO.DISK_OUT.subsink(to.right().file);
			source.depthStream().filter(VirtualFile.class::isInstance).filter(p -> p.relativePath().endsWith(".java")).forEach(v -> {
				try {
					if(this.canTransform(v.relativePath())) {
						this.visitEntry(sink, v.relativePath(), ((VirtualFile) v).newInputStream());
					}
				} catch(IOException e) {
					throw FPInternal.rethrow(e);
				}
			});
		}
	}
	
	@Override
	public void acceptMappings(List<Mappings.Namespaced> list, Remapper remapper) {
		this.remapper = remapper;
	}
	
	public boolean canTransform(String relativePath) {
		String[] names;
		return relativePath.startsWith("META-INF") && (shouldStripForFixMeta(relativePath)
		                                               || (names = names(relativePath)).length == 2 && names[1].equals("MANIFEST.MF")
		                                               || names.length == 3 && names[1].equals("services"));
	}
	
	private static String mapFullyQualifiedClassName(String name, Remapper tr) {
		assert name.indexOf('/') < 0;
		
		return tr.map(name.replace('.', '/')).replace('/', '.');
	}
	
	private static boolean shouldStripForFixMeta(String file) {
		String[] names = names(file);
		if(names.length != 2) {
			return false; // not directly inside META-INF dir
		}
		
		String fileName = names[names.length - 1];
		
		// https://docs.oracle.com/en/java/javase/12/docs/specs/jar/jar.html#signed-jar-file
		return fileName.endsWith(".SF") || fileName.endsWith(".DSA") || fileName.endsWith(".RSA") || fileName.startsWith("SIG-");
	}
	
	private static void fixManifest(Manifest manifest, Remapper remapper) {
		Attributes mainAttrs = manifest.getMainAttributes();
		
		if(remapper != null) {
			String val = mainAttrs.getValue(Attributes.Name.MAIN_CLASS);
			if(val != null) {
				mainAttrs.put(Attributes.Name.MAIN_CLASS, mapFullyQualifiedClassName(val, remapper));
			}
			
			val = mainAttrs.getValue("Launcher-Agent-Class");
			if(val != null) {
				mainAttrs.put("Launcher-Agent-Class", mapFullyQualifiedClassName(val, remapper));
			}
		}
		
		mainAttrs.remove(Attributes.Name.SIGNATURE_VERSION);
		
		for(Iterator<Attributes> it = manifest.getEntries().values().iterator(); it.hasNext(); ) {
			Attributes attrs = it.next();
			
			for(Iterator<Object> it2 = attrs.keySet().iterator(); it2.hasNext(); ) {
				Attributes.Name attrName = (Attributes.Name) it2.next();
				String name = attrName.toString();
				
				if(name.endsWith("-Digest") || name.contains("-Digest-") || name.equals("Magic")) {
					it2.remove();
				}
			}
			
			if(attrs.isEmpty()) {
				it.remove();
			}
		}
	}
	
	private static void fixServiceDecl(BufferedReader reader, BufferedWriter writer, Remapper remapper) throws IOException {
		String line;
		
		while((line = reader.readLine()) != null) {
			int end = line.indexOf('#');
			if(end < 0) {
				end = line.length();
			}
			
			// trim start+end to skip ' ' and '\t'
			
			int start = 0;
			char c;
			
			while(start < end && ((c = line.charAt(start)) == ' ' || c == '\t')) {
				start++;
			}
			
			while(end > start && ((c = line.charAt(end - 1)) == ' ' || c == '\t')) {
				end--;
			}
			
			if(start == end) {
				writer.write(line);
			} else {
				writer.write(line, 0, start);
				writer.write(mapFullyQualifiedClassName(line.substring(start, end), remapper));
				writer.write(line, end, line.length() - end);
			}
			
			writer.newLine();
		}
	}
	
	static String[] names(String path) {
		if(path.isEmpty()) {
			return EMPTY;
		}
		if(path.charAt(0) == '/') {
			path = path.substring(1);
		}
		if(path.isEmpty()) {
			return EMPTY;
		}
		if(path.charAt(path.length() - 1) == '/') {
			path = path.substring(0, path.length() - 1);
		}
		if(path.isEmpty()) {
			return EMPTY;
		}
		return path.split("/");
	}
	
	private boolean visitEntry(VirtualSink sink, String path, InputStream contents) throws IOException {
		String[] names = names(path);
		if(names.length == 2 && names[names.length - 1].equals("MANIFEST.MF")) {
			Manifest manifest = new Manifest(contents);
			fixManifest(manifest, remapper);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			manifest.write(baos);
			sink.write(sink.outputFile(path), ByteBuffer.wrap(baos.toByteArray()));
		} else if(names.length == 3 && names[1].equals("services")) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(contents)); BufferedWriter writer =
					                                                                                 new BufferedWriter(new OutputStreamWriter(
					baos))) {
				fixServiceDecl(reader, writer, remapper);
			}
			sink.write(sink.outputFile(path), ByteBuffer.wrap(baos.toByteArray()));
		}
		return true;
	}
}
