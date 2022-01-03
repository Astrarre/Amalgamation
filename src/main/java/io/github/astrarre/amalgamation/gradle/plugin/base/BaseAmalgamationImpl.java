package io.github.astrarre.amalgamation.gradle.plugin.base;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.AccessWidenerDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.URLDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.decomp.DecompileDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.RemapDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.RemapDependencyConfig;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.MappingTarget;
import io.github.astrarre.amalgamation.gradle.ide.eclipse.ConfigureEclipse;
import io.github.astrarre.amalgamation.gradle.ide.eclipse.EclipseExtension;
import io.github.astrarre.amalgamation.gradle.ide.idea.ConfigIdea;
import io.github.astrarre.amalgamation.gradle.ide.idea.IdeaExtension;
import io.github.astrarre.amalgamation.gradle.plugin.base.mvn.ConfigurationExcluder;
import io.github.astrarre.amalgamation.gradle.plugin.base.mvn.MvnMetaReader;
import io.github.astrarre.amalgamation.gradle.plugin.base.mvn.NotationFixer;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.Lazy;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.tasks.GenerateModuleMetadata;
import org.jetbrains.annotations.NotNull;

public class BaseAmalgamationImpl implements BaseAmalgamation {
	public final Project project;
	public final Logger logger;

	public BaseAmalgamationImpl(Project project) {
		this.project = project;
		this.logger = project.getLogger();
	}

	void transformModuleJson(MavenPublication publication, MvnMetaReader.DependencyVisitor visitor) {
		final String publicationName = publication.getName();
		String descriptorTaskName = "generateMetadataFileFor" + capitalize(publicationName) + "Publication";
		var named = this.project.getTasks().named(descriptorTaskName, GenerateModuleMetadata.class);
		named.configure(module -> module.appendParallelSafeAction(task -> {
			MvnMetaReader.visitDependencies(module.getOutputFile().getAsFile().get().toPath(), visitor);
		}));
	}

	@Override
	public void fixPom(MavenPublication publication) {
		this.transformModuleJson(publication, NotationFixer.INSTANCE);
		publication.pom(pom -> pom.withXml(xml -> MvnMetaReader.visitDependencies(xml, NotationFixer.INSTANCE)));
	}

	@Override
	public void excludeConfiguration(MavenPublication publication, Configuration configuration) {
		ConfigurationExcluder visitor = new ConfigurationExcluder(configuration);
		this.transformModuleJson(publication, visitor);
		publication.pom(pom -> pom.withXml(xml -> MvnMetaReader.visitDependencies(xml, visitor)));
	}

	private static String capitalize(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	@Override
	public <T> Provider<T> provideLazy(Supplier<T> action) {
		return this.project.provider(Lazy.of(action));
	}

	@Override
	public List<File> resolve(Iterable<Dependency> dependency) {
		return AmalgIO.resolve(this.project, dependency);
	}

	@Override
	public List<File> resolveWithSources(Iterable<Dependency> dependency) {
		List<File> files = AmalgIO.resolveSources(this.project, dependency).stream().map(Path::toFile).toList();
		List<File> resolve = AmalgIO.resolve(this.project, dependency);
		resolve.addAll(files);
		return resolve;
	}

	@Override
	public Provider<FileCollection> sources(Object object) {
		return this.sources0(this.project.getDependencies().create(object));
	}

	@Override
	public Provider<FileCollection> sources(Object object, Closure<ModuleDependency> config) {
		return this.sources0(this.project.getDependencies().create(object, config));
	}

	@Override
	public Dependency url(String url) {
		return new URLDependency(this.project, url);
	}

	@Override
	public IdeaExtension idea() throws IllegalStateException {
		return this.getExtension("idea", "idea", "", () -> ConfigIdea.IDEA_EXTENSION);
	}

	@Override
	public EclipseExtension eclipse() throws IllegalStateException {
		return this.getExtension("eclipse", "eclipse", "", () -> ConfigureEclipse.extension);
	}

	@Override
	public Object decompile(Action<DecompileDependency> configure) {
		DecompileDependency dependency1 = new DecompileDependency(this.project);
		configure.execute(dependency1);
		return dependency1;
	}

	@Override
	public Object accessWidener(Object depNotation, Action<AccessWidenerDependency> configure) throws IOException {
		AccessWidenerDependency dependency = new AccessWidenerDependency(this.project, depNotation);
		configure.execute(dependency);
		return dependency;
	}

	@Override
	public MappingTarget mappings(Object depNotation, String from, String to) {
		return new MappingTarget(this.project, this.project.getDependencies().create(depNotation), from, to);
	}

	@Override
	public MappingTarget mappings(Object depNotation, String from, String to, Closure<ModuleDependency> config) {
		return new MappingTarget(this.project, this.project.getDependencies().create(depNotation, config), from, to);
	}

	@Override
	public Object map(Action<RemapDependencyConfig> mappings) throws IOException {
		RemapDependency dependency = new RemapDependency(this.project);
		mappings.execute(dependency.config);
		return dependency;
	}

	private Provider<FileCollection> sources0(Dependency dependency) {
		return this.provideLazy(() -> this.project.files((Object[]) AmalgIO.resolveSources(this.project, List.of(dependency))
				.stream()
				.map(Path::toFile)
				.toArray(File[]::new)));
	}

	@NotNull
	private <T> T getExtension(String depName, String dep, String version, Supplier<T> extension) {
		T ext = extension.get();
		if(ext == null) {
			throw new IllegalStateException(String.format("%s plugin not found! \n\tplugins {\n\t\tid '%s' %s\n\t}", depName, dep, version));
		}
		return ext;
	}
}
