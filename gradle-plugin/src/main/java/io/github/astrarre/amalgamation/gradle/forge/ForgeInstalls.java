package io.github.astrarre.amalgamation.gradle.forge;

import net.minecraftforge.installer.actions.ClientInstall;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.actions.ServerInstall;
import net.minecraftforge.installer.json.Install;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Predicate;

public class ForgeInstalls {
	public static class Client extends ClientInstall {
		private final Path libraries;
		private final File client;
		public Client(Install profile, ProgressCallback monitor, Path libraries, File client) {
			super(profile, monitor);
			this.libraries = libraries;
			this.client = client;
		}

		@Override
		public boolean run(File target, Predicate<String> optionals) {
			return this.processors.process(this.libraries.toFile(), this.client);
		}
	}

	public static class Server extends ServerInstall {
		private final Path libraries;
		private final File client;

		public Server(Install profile, ProgressCallback monitor, Path libraries, File client) {
			super(profile, monitor);
			this.libraries = libraries;
			this.client = client;
		}

		@Override
		public boolean run(File target, Predicate<String> optionals) {
			return this.processors.process(this.libraries.toFile(), this.client);
		}
	}
}