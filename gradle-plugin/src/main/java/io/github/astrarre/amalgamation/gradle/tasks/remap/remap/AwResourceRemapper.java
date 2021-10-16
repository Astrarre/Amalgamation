package io.github.astrarre.amalgamation.gradle.tasks.remap.remap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.devtech.zipio.impl.util.U;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerRemapper;
import net.fabricmc.accesswidener.AccessWidenerWriter;
import net.fabricmc.accesswidener.ForwardingVisitor;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;

public class AwResourceRemapper implements OutputConsumerPath.ResourceRemapper {
	private static final Logger LOGGER = Logger.getLogger(AwResourceRemapper.class.getSimpleName());
	final String destNamespace;
	boolean hasResolvedAwFromFMJ;
	Path awResolved;

	public AwResourceRemapper(String namespace) {
		this.destNamespace = namespace;
	}

	@Override
	public boolean canTransform(TinyRemapper remapper, Path relativePath) {
		if(this.initResolved(relativePath)) {
			try {
				return relativePath.toRealPath().equals(this.awResolved);
			} catch(IOException e) {
				throw U.rethrow(e);
			}
		} else {
			String path = relativePath.toString();
			return path.endsWith(".aw") || path.endsWith(".accesswidener") || path.endsWith(".accessWidener");
		}
	}

	private boolean initResolved(Path relativePath) {
		if(!this.hasResolvedAwFromFMJ) {
			Path root = relativePath.toAbsolutePath().getRoot();
			Path fmj = root.resolve("fabric.mod.json");
			Gson gson = new Gson();
			try(BufferedReader reader = Files.newBufferedReader(fmj)) {
				JsonObject object = gson.fromJson(reader, JsonObject.class);
				JsonPrimitive aw = object.getAsJsonPrimitive("accessWidener");
				if(aw != null) {
					Path accessWidener = root.resolve(aw.getAsString()).toRealPath();
					if(Files.exists(accessWidener)) {
						this.awResolved = accessWidener;
					} else {
						LOGGER.warning("Unable to locate " + aw + ", defaulting to extension detection (.aw, .accesswidener, .accessWidener)");
					}
				}
			} catch(IOException e) {
				LOGGER.warning("Unable to locate fabric.mod.json, defaulting to extension detection (.aw, .accesswidener, .accessWidener)");
			}
			this.hasResolvedAwFromFMJ = true;
		}
		return this.awResolved != null;
	}

	@Override
	public void transform(Path destinationDirectory, Path relativePath, InputStream input, TinyRemapper remapper) throws IOException {
		AccessWidenerWriter writer = new AccessWidenerWriter();
		AccessWidenerRemapper accessRemapper = new AccessWidenerRemapper(writer, remapper.getEnvironment().getRemapper(), null, null);
		ForwardingVisitor visitor = new ForwardingVisitor(accessRemapper) {
			@Override
			public void visitHeader(String namespace) {
				writer.visitHeader(AwResourceRemapper.this.destNamespace);
			}
		};
		AccessWidenerReader reader = new AccessWidenerReader(visitor);
		byte[] data = input.readAllBytes();
		reader.read(data);
		Path output = destinationDirectory.resolve(relativePath);
		Files.write(output, writer.write());
	}
}
