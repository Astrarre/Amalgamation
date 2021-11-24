package io.github.astrarre.amalgamation.gradle.dependencies.transform;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.hash.Hasher;
import groovy.lang.Closure;
import io.github.astrarre.amalgamation.gradle.dependencies.ZipProcessDependency;
import io.github.astrarre.amalgamation.gradle.dependencies.filters.SourcesOutput;
import io.github.astrarre.amalgamation.gradle.dependencies.transform.inputs.InputType;
import io.github.astrarre.amalgamation.gradle.dependencies.util.DependencyList;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.ZipProcessable;
import io.github.astrarre.amalgamation.gradle.utils.func.AmalgDirs;
import net.devtech.zipio.OutputTag;
import net.devtech.zipio.processes.ZipProcessBuilder;
import net.devtech.zipio.stage.TaskTransform;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({
		"unchecked",
		"rawtypes"
})
public class TransformDependency<C extends TransformConfiguration<C, D>, D extends TransformDependency.Transformer<C>> extends ZipProcessDependency
		implements TransformConfiguration<C, D> {
	public static final Logger LOGGER = Logger.getLogger("Amalg-Transform");
	public final List<SingleTransformDependency> dependencies = new ArrayList<>();
	final ListMultimap<InputType<?, ?>, Input<?>> inputs = ArrayListMultimap.create();
	final D processor;
	AmalgDirs cache = null;

	public TransformDependency(Project project, String group, String name, String version, D processor) {
		super(project, group, name, version);
		this.processor = processor;
		processor.setParent(this);
	}

	@SuppressWarnings("unchecked")
	public static <C extends TransformConfiguration<C, T>, T extends TransformDependency.Transformer<C>> TransformDependency<C, T> create(Project project,
			T transformer,
			Action<C> configure) throws IOException {
		var dep = new TransformDependency<>(project, "io.github.astrarre.amalgamation", "transformed-dependency", "0.0.0", transformer);
		var instance = Proxy.newProxyInstance(TransformDependency.class.getClassLoader(),
				new Class[] {transformer.configurationHelper()},
				(proxy, method, args) -> {
					if(method.getDeclaringClass() == TransformConfiguration.class) {
						return method.invoke(dep, args);
					} else {
						return InvocationHandler.invokeDefault(proxy, method, args);
					}
				});
		configure.execute((C) instance);
		dep.postConfigure();
		return dep;
	}

	@Override
	public void hashInputs(Hasher hasher) throws IOException {
		this.processor.hash(hasher);
		for(var input : this.inputs.values()) {
			this.hashDep(hasher, input.dependency);
		}
	}

	@Override
	protected Path evaluatePath(byte[] hash) throws IOException {
		return this.cache.transforms(this.project).resolve(AmalgIO.b64(hash));
	}

	@Override
	public <T> List<T> inputAll(InputType<D, T> type, List<?> list) {
		List<T> inputs = new ArrayList<>();
		for(Object dependency : list) {
			inputs.add(this.getT(type, this.of(dependency)));
		}
		return inputs;
	}

	@Override
	public <T> T input(InputType<D, T> type, Object depNotation) {
		if(depNotation instanceof DependencyList l && l.callback != null) {
			return this.getT(type, l.callback);
		}
		if(depNotation instanceof List<?> l) {
			return this.applyMulti(type, l);
		}
		Dependency of = this.of(depNotation);
		return this.getT(type, of);
	}

	@Override
	public <T> T input(InputType<D, T> type, Object depNotation, Closure<ModuleDependency> dep) {
		if(depNotation instanceof DependencyList) {
			LOGGER.warning("Can't chain amalg dependencies with the module dependency configuring closure");
		}
		if(depNotation instanceof List<?> l) {
			return this.applyMulti(type, l);
		}
		Dependency of = this.of(depNotation, dep);
		return this.getT(type, of);
	}

	@Override
	public D getTransformer() {
		return this.processor;
	}

	public void postConfigure() throws IOException {
		byte[] current = this.getCurrentHash();
		for(SingleTransformDependency dependency : this.dependencies) {
			Hasher hasher = AmalgIO.HASHING.newHasher();
			hasher.putBytes(current);
			this.hashDep(hasher, dependency.input);
			dependency.append(AmalgIO.hash(hasher));
		}
	}

	@Nullable
	private <T> T applyMulti(InputType<D, T> type, List<?> l) {
		BinaryOperator<T> operator = type.collector();
		List<T> ts = this.inputAll(type, l);
		if(ts.isEmpty()) {
			return null;
		}

		Iterator<T> iter = ts.iterator();
		T combined = iter.next();
		while(iter.hasNext()) {
			combined = operator.apply(combined, iter.next());
		}

		return combined;
	}

	private <T> T getT(InputType<D, T> type, Dependency of) {
		SingleTransformDependency dependency;
		AmalgDirs location = type.cacheLocation();
		if(location != null) {
			dependency = new SingleTransformDependency(of.getGroup(), of.getName(), of.getVersion(), of, this);
			this.dependencies.add(dependency);
		} else {
			dependency = null;
		}

		T value = type.apply(this.project, this.processor, dependency, of);
		this.inputs.put(type, new Input<>(this.project, type, of, dependency, value));

		if(location != null && (this.cache == null || location.ordinal() < this.cache.ordinal())) {
			this.cache = location;
		}
		return value;
	}

	@Override
	protected void add(TaskInputResolver resolver, ZipProcessBuilder process, Path resolvedPath, boolean isOutdated) throws IOException {
		if(isOutdated) {
			this.processor.configure(this.project, new Inputs(this.inputs), process);
		} else {
			for(var input : this.inputs.values()) {
				if(input.representation != null) {
					input.representation.outputs.clear();
					ZipProcessable.getOutputs(this.project, input.dependency)
							.map(input::resolve)
							.peek(input.representation.outputs::add)
							.forEach(process::addProcessed);
				}
			}
		}
	}

	public interface Transformer<T extends TransformConfiguration<T, ?>> {
		/**
		 * an interface class that contains methods that return all the valid input types for this processor
		 */
		Class<T> configurationHelper();

		void configure(Project project, Inputs inputs, ZipProcessBuilder builder) throws IOException;

		void hash(Hasher hasher) throws IOException;

		default void setParent(TransformDependency<?, ?> dependency) {}
	}

	public record Inputs(ListMultimap<InputType<?, ?>, Input<?>> delegate) {
		public Inputs(ListMultimap<InputType<?, ?>, Input<?>> delegate) {
			this.delegate = ImmutableListMultimap.copyOf(delegate);
		}

		public <T> List<Input<T>> get(InputType<?, T> type) {
			return (List) this.delegate.get(type);
		}

		public <T> List<Input<? extends T>> getAll(InputType<?, ? extends T>... types) {
			List<Input<? extends T>> inputs = new ArrayList<>();
			for(InputType<?, ? extends T> type : types) {
				inputs.addAll(this.get(type));
			}
			return inputs;
		}

		public <T> List<Input<T>> getByType(Class<? extends InputType<?, T>> type) {
			List<Input<T>> inputs = new ArrayList<>();
			this.delegate.forEach((type1, input) -> {
				if(type.isInstance(input.input)) {
					inputs.add((Input<T>) input);
				}
			});
			return inputs;
		}
	}

	public DependencyList getDependencies() {
		List<Object> objects = new ArrayList<>();
		for(SingleTransformDependency dependency : this.dependencies) {
			String group = dependency.getGroup();
			String str;
			if(group == null) {
				str = dependency.getName() + ":" + dependency.getVersion();
			} else {
				str = group + ":" + dependency.getName() + ":" + dependency.getVersion();
			}
			objects.add(str);
		}
		return new DependencyList(objects, this);
	}

	public record Input<T>(Project project, InputType<?, ?> input, Dependency dependency, SingleTransformDependency representation, T value) {
		public OutputTag resolve(OutputTag tag) {
			Path transforms = this.input.cacheLocation().transforms(this.project).resolve(this.input.hint());
			try {
				Files.createDirectories(transforms);
			} catch(IOException e) {
				throw new RuntimeException(e);
			}

			OutputTag output = tag(tag, transforms.resolve(this.representation.jarName(tag instanceof SourcesOutput ? "sources" : null)));
			this.representation.outputs.add(output);
			return output;
		}

		public List<TaskTransform> appendInputs(ZipProcessBuilder builder) throws IOException {
			this.representation.outputs.clear();
			return this.appendInputs(builder, this::resolve);
		}

		public List<TaskTransform> appendInputs(ZipProcessBuilder builder, UnaryOperator<OutputTag> resolver) throws IOException {
			this.representation.outputs.clear();
			return ZipProcessable.apply(this.project, builder, this.dependency, resolver);
		}
	}
}
