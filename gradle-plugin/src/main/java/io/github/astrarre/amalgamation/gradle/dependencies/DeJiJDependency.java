package io.github.astrarre.amalgamation.gradle.dependencies;

public class DeJiJDependency {} /*extends AbstractSelfResolvingDependency {
	public final List<Dependency> dependencies;

	public DeJiJDependency(Project project, String name) {
		this(project, name, new ArrayList<>());
	}

	public DeJiJDependency(Project project, String name, List<Dependency> dependencies) {
		super(project, "io.github.astrarre.amalgamation", name, "0.0.0");
		this.dependencies = dependencies;
	}

	public void add(Object dependency) {
		this.dependencies.add(this.project.getDependencies().create(dependency));
	}

	public void add(Object dependency, Closure<ModuleDependency> config) {
		this.dependencies.add(this.project.getDependencies().create(dependency, config));
	}

	@Override
	protected Iterable<Path> resolvePaths() {
		// todo this can be a zip process
		List<File> toProcess = new ArrayList<>();
		Set<File> doNotRemove = new HashSet<>();
		for (File file : this.resolve(this.dependencies)) {
			toProcess.add(file);
			doNotRemove.add(file);
		}

		while (!toProcess.isEmpty()) {
			for (int i = toProcess.size() - 1; i >= 0; i--) {
				File process = toProcess.remove(i);
				Path cache = AmalgIO.projectCache(this.project).resolve("de-jij").resolve(this.name).resolve(AmalgIO.hash(Collections.singleton(process)));
				DeJiJCachedFile cachedFile = new DeJiJCachedFile(cache, process);
				for(Path path : UnsafeIterable.walkFiles(cachedFile.getOutput())) {
					if(!path.endsWith("original.jar")) {
						toProcess.add(path.toFile());
					}
				}

				if(!doNotRemove.contains(process)) {
					process.delete();
				}
			}
		}
		return UnsafeIterable.walkFiles(AmalgIO.projectCache(this.project).resolve("de-jij").resolve(this.name));
	}

	@Override
	public Dependency copy() {
		return new DeJiJDependency(this.project, this.name, new ArrayList<>(this.dependencies));
	}

	public static class DeJiJCachedFile extends CachedFile {
		public final File toDeJiJ;
		public DeJiJCachedFile(Path file, File toDeJiJ) {
			super(file);
			this.toDeJiJ = toDeJiJ;
		}

		@Override
		public void hashInputs(Hasher hasher) {
			AmalgIO.hash(hasher, this.toDeJiJ);
		}

		@Override
		protected void write(Path path) throws IOException {
			Files.createDirectories(path);
			try(ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(this.toDeJiJ))); ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(path.resolve("original.jar"))))) {
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null) {
					String name = entry.getName();
					if(name.endsWith("fabric.mod.json")) {
						JsonObject object = GSON.fromJson(new InputStreamReader(zis), JsonObject.class);
						if(object.has("jars")) {
							object.remove("jars");
							zos.putNextEntry(new ZipEntry(entry.getName()));
							GSON.toJson(object, new OutputStreamWriter(zos));
							zos.closeEntry();
							break;
						}
					}
					if(name.endsWith(".jar")) {
						Path toWrite = path.resolve(name);
						Files.createDirectories(toWrite.getParent());
						Files.copy(zis, toWrite);
					} else {
						zos.putNextEntry(new ZipEntry(entry.getName()));
						AmalgIO.copy(zis, zos);
						zos.closeEntry();
					}
				}
			}
		}
	}
}*/
