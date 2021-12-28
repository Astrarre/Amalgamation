package io.github.astrarre.amalgamation.gradle.plugin.base.mvn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.astrarre.amalgamation.gradle.plugin.base.Dependent;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import net.devtech.zipio.impl.util.U;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.publish.Publication;
import org.gradle.api.publish.tasks.GenerateModuleMetadata;

public class ModuleJsonFixer implements Action<Task> {
	static final Map<Publication, ModuleJsonFixer> FIXERS = new WeakHashMap<>();

	public static ModuleJsonFixer getForPublication(Publication publication) {
		return FIXERS.computeIfAbsent(publication, $ -> new ModuleJsonFixer());
	}

	final Set<Dependent> toRemove = new HashSet<>();

	private ModuleJsonFixer() {}

	@Override
	public void execute(Task task) {
		GenerateModuleMetadata metadata = (GenerateModuleMetadata) task;
		File file = metadata.getOutputFile().getAsFile().get();
		boolean changed = false;
		JsonObject object;
		try(BufferedReader reader = Files.newBufferedReader(file.toPath())) {
			object = LauncherMeta.GSON.fromJson(reader, JsonObject.class);
			JsonArray variants = object.getAsJsonArray("variants");
			if(variants != null) {
				for(JsonElement variant : variants) {
					Set<Dependent> dependents = new HashSet<>();
					JsonObject variant_ = (JsonObject) variant;
					JsonArray dependencies = variant_.getAsJsonArray("dependencies");
					if(dependencies != null) {
						for(int i = dependencies.size() - 1; i >= 0; i--) {
							JsonObject dependency = (JsonObject) dependencies.get(i);
							JsonObject versionObject = dependency.getAsJsonObject("version");
							if(versionObject != null && dependency.has("module") && versionObject.has("requires")) {
								String version = versionObject.getAsJsonPrimitive("requires").getAsString();
								String name = dependency.getAsJsonPrimitive("module").getAsString();
								String group = dependency.has("group") ? dependency.getAsJsonPrimitive("group").getAsString() : null;
								Integer index = getIndex(version, name);
								System.out.println(version + " " + name + " " + index);
								if(index == null) {
									continue;
								}

								String trueName = name.substring(0, index);
								String trueVersion = name.substring(index + 1);
								Dependent dependent = new Dependent(group, trueName, trueVersion);
								if(dependents.add(dependent) && !trueName.equals("merged")) { // todo unhardcode
									dependency.addProperty("module", trueName);
									versionObject.addProperty("requires", trueVersion);
								} else {
									dependencies.remove(i);
								}
								changed = true;
							}
						}
					}
				}
			}
		} catch(IOException e) {
			throw U.rethrow(e);
		}

		if(changed) {
			try(BufferedWriter reader = Files.newBufferedWriter(file.toPath())) {
				LauncherMeta.GSON.toJson(object, reader);
			} catch(IOException e) {
				throw U.rethrow(e);
			}
		}
	}

	public static Integer getIndex(String version, String name) {
		int i = version.lastIndexOf('_');
		if(i == -1) {
			return null;
		}
		String indexStr = version.substring(i + 1);
		int index;
		try {
			index = Integer.parseInt(indexStr);
		} catch(NumberFormatException e) {
			return null;
		}

		if(name.charAt(index) != '_') {
			return null;
		}
		return index;
	}
}
