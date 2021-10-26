package io.github.astrarre.amalgamation.gradle.sources;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ConfiguredModuleComponentRepository;
import org.gradle.api.internal.artifacts.repositories.AbstractResolutionAwareArtifactRepository;
import org.gradle.api.internal.artifacts.repositories.DefaultFlatDirArtifactRepository;
import org.gradle.api.internal.artifacts.repositories.descriptor.FlatDirRepositoryDescriptor;
import org.gradle.api.internal.artifacts.repositories.descriptor.RepositoryDescriptor;
import org.gradle.api.internal.artifacts.repositories.resolver.ExternalResourceResolver;
import org.gradle.api.internal.artifacts.repositories.resolver.IvyResolver;
import org.gradle.api.internal.artifacts.repositories.resolver.ResourcePattern;
import org.gradle.internal.component.external.model.ModuleComponentArtifactMetadata;
import org.gradle.internal.component.model.IvyArtifactName;
import org.gradle.internal.resource.ExternalResourceName;
import org.jetbrains.annotations.NotNull;


public class AmalgSourcesRepository extends AbstractResolutionAwareArtifactRepository {
	static final MethodHandle ADD_ARTIFACT_PATTERN;

	static {
		try {
			ADD_ARTIFACT_PATTERN = MethodHandles.privateLookupIn(ExternalResourceResolver.class, MethodHandles.lookup())
					.findVirtual(ExternalResourceResolver.class, "setArtifactPatterns", MethodType.methodType(void.class, List.class));
		} catch(NoSuchMethodException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	IvyResolver resolver;

	public AmalgSourcesRepository(Project project) {
		super(project.getObjects());
		this.setName("Amalgamation Source Resolver");
		project.getRepositories().flatDir(dir -> {
			dir.content(c -> c.includeGroup(""));

			dir.setDirs(Set.of(project.getBuildDir()));
			var resolver = this.resolver = (IvyResolver) ((DefaultFlatDirArtifactRepository) dir).createResolver();
			Path of = Path.of(
					"C:\\Users\\devan\\Documents\\Java\\f2bb\\Amalgamation\\test\\build\\amalgamation-caches\\transforms\\AccessWidenerInput" +
					"\\wycMyZQyEh1PfjpycxA61DROlQgNcGA1nMwyMgAwqCM=\\gson-1.1-sources.jar");
			Path src = Path.of("C:\\Users\\devan\\Documents\\Java\\f2bb\\Amalgamation\\test\\build\\amalgamation-caches\\transforms" +
			                   "\\AccessWidenerInput\\HJTQyY1o5e9vnDDF9EyGPa5yPXoVlTM9qQQGp322GG8=\\gson-1.1.jar");
			Path dst = Path.of("C:\\Users\\devan\\.gradle\\caches\\modules-2\\files-2.1\\com.google.code.gson\\gson\\1" +
			                   ".1\\cbf51ba4a88ae596f9b3acf937779a57ce602f25\\gson-1.1-sources.jar");
			try {
				ADD_ARTIFACT_PATTERN.invoke(resolver, List.of(new SingleFile(src, of), new SingleFile(src, dst)));

			} catch(Throwable e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public ConfiguredModuleComponentRepository createResolver() {
		return this.resolver;
	}

	@Override
	protected RepositoryDescriptor createDescriptor() {
		return new FlatDirRepositoryDescriptor("Amalgamation Source Provider", List.of());
	}

	static class SingleFile implements ResourcePattern {
		final Path src, path;

		public SingleFile(Path src, Path path) {
			this.src = src;
			this.path = path.toAbsolutePath();
		}

		@Override
		public String getPattern() {
			return "copemaldseethecrymanagehandlesubsistfaceaddressconfrontaccept";
		}

		@Override
		public ExternalResourceName getLocation(ModuleComponentArtifactMetadata artifact) {
			if(artifact.getName().getClassifier() != null) {
				return new ExternalResourceName(this.path.getParent().toUri(), this.path.getFileName().toString());
			} else {
				return new ExternalResourceName(this.src.getParent().toUri(), this.src.getFileName().toString());
			}
		}

		@Override
		public ExternalResourceName toVersionListPattern(ModuleIdentifier module, IvyArtifactName artifact) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ExternalResourceName toModulePath(ModuleIdentifier moduleIdentifier) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ExternalResourceName toModuleVersionPath(ModuleComponentIdentifier componentIdentifier) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isComplete(@NotNull ModuleIdentifier moduleIdentifier) {
			return false;
		}

		@Override
		public boolean isComplete(@NotNull ModuleComponentIdentifier componentIdentifier) {
			return false;
		}

		@Override
		public boolean isComplete(ModuleComponentArtifactMetadata id) {
			return id.getName().getName().equals("transformed-dependency") && !Objects.equals(id.getName().getClassifier(), "javadoc");
		}
	}

}
