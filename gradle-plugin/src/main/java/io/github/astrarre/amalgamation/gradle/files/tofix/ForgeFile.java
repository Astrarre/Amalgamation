package io.github.astrarre.amalgamation.gradle.files.tofix;

public class ForgeFile {} /*extends CachedFile {
	public static final String MINECRAFT_VERSION;
	public static final String FORGE_VERSION;

	static {
		try {
			Properties properties = new Properties();
			properties.load(ForgeUtil.class.getResourceAsStream("/gradle_data.properties"));
			MINECRAFT_VERSION = properties.getProperty("minecraft_version");
			FORGE_VERSION = properties.getProperty("forge_version");
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public final boolean client;
	public final Dependency minecraft;
	public final LibrariesDependency libraries;

	public ForgeFile(boolean client, MinecraftDependency minecraft, LibrariesDependency libraries, String forgeVersion) {
		super(() -> {
			String vers = minecraft.getVersion() + "-" + forgeVersion;
			return Paths.get(libraries.librariesDirectory,
					"net",
					"minecraftforge",
					"forge",
					vers,
					client ? "forge-" + vers + "-client.jar" : "forge-" + vers + "-server.jar");
		}, String.class);
		this.client = client;
		this.minecraft = minecraft;
		this.libraries = libraries;
	}

	@Override
	protected @Nullable String writeIfOutdated(Path path, @Nullable String currentData) throws Throwable {
		File file = AmalgIO.resolve(this.libraries.project, this.minecraft);
		String str = AmalgIO.hash(Collections.singleton(file));
		if(!str.equals(currentData)) {
			File libs = new File(this.libraries.librariesDirectory);
			if(this.client) {
				ForgeUtil.getForgeClient(libs, file);
			} else {
				ForgeUtil.getForgeServer(libs, file);
			}
		}
		return null;
	}
}*/