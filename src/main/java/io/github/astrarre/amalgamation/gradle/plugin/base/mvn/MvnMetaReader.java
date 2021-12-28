package io.github.astrarre.amalgamation.gradle.plugin.base.mvn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import groovy.util.Node;
import groovy.util.NodeList;
import io.github.astrarre.amalgamation.gradle.plugin.base.Dependent;
import io.github.astrarre.amalgamation.gradle.utils.LauncherMeta;
import net.devtech.zipio.impl.util.U;
import org.gradle.api.XmlProvider;
import org.jetbrains.annotations.Nullable;

public class MvnMetaReader {
	public static void visitDependencies(Path json, DependencyVisitor visitor) {
		boolean changed = false;
		JsonObject object;
		try(BufferedReader reader = Files.newBufferedReader(json)) {
			object = LauncherMeta.GSON.fromJson(reader, JsonObject.class);
			JsonArray variants = object.getAsJsonArray("variants");
			if(variants == null) {
				return;
			}
			for(JsonElement variant : variants) {
				Set<Dependent> dependents = new HashSet<>();
				JsonObject variant_ = (JsonObject) variant;
				JsonArray dependencies = variant_.getAsJsonArray("dependencies");
				if(dependencies == null) {
					continue;
				}
				for(int i = dependencies.size() - 1; i >= 0; i--) {
					JsonObject dependency = (JsonObject) dependencies.get(i);
					JsonObject versObj = dependency.getAsJsonObject("version");
					String group = dependency.has("group") ? dependency.get("group").getAsString() : null;
					String name = dependency.getAsJsonPrimitive("module").getAsString();
					String version = versObj != null && versObj.has("requires") ? versObj.get("requires").getAsString() : null;
					String[] mut = {
							name,
							version
					};

					if(visitor.apply(group, name, version, (name1, version1) -> {
						mut[0] = name1;
						mut[1] = version1;
					})) {
						dependencies.remove(i);
						changed = true;
						continue;
					}

					if(name.equals(mut[0]) && Objects.equals(version, mut[1])) {
						continue;
					}

					name = mut[0];
					version = mut[1];

					Dependent dependent = new Dependent(group, name, version);
					if(dependents.add(dependent)) {
						dependency.addProperty("module", name);
						if(version != null) {
							versObj.addProperty("requires", version);
						}
					} else {
						dependencies.remove(i);
					}
					changed = true;
				}
			}
		} catch(IOException e) {
			throw U.rethrow(e);
		}

		if(changed) {
			try(BufferedWriter reader = Files.newBufferedWriter(json)) {
				LauncherMeta.GSON.toJson(object, reader);
			} catch(IOException e) {
				throw U.rethrow(e);
			}
		}
	}

	public static void visitDependencies(XmlProvider xml, DependencyVisitor visitor) {
		Node dependencies = Iterables.getOnlyElement((List<Node>) xml.asNode().get("dependencies"));
		Set<Dependent> dependents = new HashSet<>();
		for(Node node : (List<Node>) dependencies.get("node")) {
			NodeList list = (NodeList) node.get("groupId");
			Node element = (Node) Iterables.getOnlyElement(list, null);
			String group = element == null ? null : element.text();
			Node artifactId = (Node) Iterables.getOnlyElement((NodeList) node.get("artifactId"));
			String name = artifactId.text();
			Node version = (Node) Iterables.getOnlyElement((NodeList) node.get("version"), null);
			String vers = version == null ? null : version.text();

			String[] mut = {
					name,
					vers
			};
			if(visitor.apply(group, name, vers, (name1, version1) -> {
				mut[0] = name1;
				mut[1] = version1;
			})) {
				dependencies.remove(node);
				continue;
			}

			if(name.equals(mut[0]) && Objects.equals(vers, mut[1])) {
				continue;
			}
			name = mut[0];
			vers = mut[1];

			Dependent dependent = new Dependent(group, name, vers);
			if(dependents.add(dependent)) {
				artifactId.setValue(name);
				if(version != null) {
					version.setValue(vers);
				}
			} else {
				dependencies.remove(node);
			}
		}
	}

	interface Mutator {
		void set(String name, String version);
	}

	public interface DependencyVisitor {
		boolean apply(@Nullable String group, String name, @Nullable String version, Mutator mutator);
	}
}
