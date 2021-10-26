package io.github.astrarre.amalgamation.gradle.dependencies.transform;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.common.hash.Hasher;
import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.ZipProcessDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.inputs.InputType;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.ZipProcessable;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.stage.TaskTransform;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;

public class TransformDependency extends ZipProcessDependency implements TransformConfiguration {
	final List<Input<?>> inputs = new ArrayList<>();
	final Transformer<?> processor;
	boolean containsLocal;

	public TransformDependency(Project project, String group, String name, String version, Transformer<?> processor) {
		super(project, group, name, version);
		this.processor = processor;
		processor.setParent(this);
	}

	@SuppressWarnings("unchecked")
	public static <T extends TransformConfiguration> TransformDependency create(Project project, Transformer<T> transformer, Action<T> configure) {
		var dep = new TransformDependency(project, "io.github.astrarre.amalgamation", "transformed-dependency", "0.0.0", transformer);

		var instance = Proxy.newProxyInstance(TransformDependency.class.getClassLoader(),
				new Class[] {transformer.configurationHelper()},
				(proxy, method, args) -> {
					if(method.getDeclaringClass() == TransformConfiguration.class) {
						return method.invoke(dep, args);
					} else {
						return InvocationHandler.invokeDefault(proxy, method, args);
					}
				});
		configure.execute((T) instance);
		return dep;
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		this.processor.hash(hasher);
		for(Input<?> input : this.inputs) {
			this.hashDep(hasher, input.dependency);
		}
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws IOException {
		return AmalgIO.cache(this.project, !this.containsLocal).resolve("transforms").resolve(AmalgIO.b64(hash));
	}

	@Override
	public <T> T input(InputType<T> type, Object depNotation) {
		Dependency of = this.of(depNotation);
		return getT(type, of);
	}

	@Override
	public <T> T input(InputType<T> type, Object depNotation, Closure<ModuleDependency> dep) {
		Dependency of = this.of(depNotation, dep);
		return this.getT(type, of);
	}

	private <T> T getT(InputType<T> type, Dependency of) {
		T value = this.processor.process(this.project, of, type);
		if(type.getCacheLocation() != InputType.CacheLocation.NONE) {
			this.inputs.add(new Input<>(this.project, type, of, value));
		}
		this.containsLocal |= type.getCacheLocation() == InputType.CacheLocation.PROJECT;
		return value;
	}

	@Override
	protected void add(TaskInputResolver resolver, ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException {
		if(isOutdated) {
			this.processor.configure(this.inputs, process);
		} else {
			for(Input<?> input : this.inputs) {
				ZipProcessable.getOutputs(this.project, input.dependency)
						.map(Path::toAbsolutePath)
						.map(input::resolve)
						.forEach(process::addProcessed);
			}
		}
	}

	public interface Transformer<T extends TransformConfiguration> {
		/**
		 * an interface class that contains methods that return all the valid input types for this processor
		 */
		Class<T> configurationHelper();

		/**
		 * @throws IllegalArgumentException if not valid input type
		 */
		<V> V process(Project project, Dependency dependency, InputType<V> type) throws IllegalArgumentException;

		void configure(List<Input<?>> inputs, ZipProcessBuilder builder) throws IOException;

		void hash(Hasher hasher);

		default void setParent(TransformDependency dependency) {}
	}

	public record Input<T>(Project project, InputType<T> input, Dependency dependency, T value) {
		public Path resolve(Path path) {
			Path transforms = AmalgIO.cache(this.project, this.input.getCacheLocation().isGlobal())
					.resolve("transforms")
					.resolve(this.input.hint())
					.resolve(AmalgIO.hash(path));
			try {
				Files.createDirectories(transforms);
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
			return transforms.resolve(path.getFileName().toString());

		}

		public List<TaskTransform> appendInputs(ZipProcessBuilder builder) throws IOException {
			return ZipProcessable.apply(this.project, builder, this.dependency, this::apply);
		}

		private OutputTag apply(OutputTag u) {return new OutputTag(this.resolve(u.getVirtualPath()));}
	}
}
