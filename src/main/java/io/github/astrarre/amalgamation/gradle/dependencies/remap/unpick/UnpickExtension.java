package io.github.astrarre.amalgamation.gradle.dependencies.remap.unpick;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import daomephsta.unpick.api.ConstantUninliner;
import daomephsta.unpick.impl.constantmappers.datadriven.DataDrivenConstantMapper;
import daomephsta.unpick.impl.representations.ReplacementInstructionGenerator;
import daomephsta.unpick.impl.representations.TargetMethods;
import io.github.astrarre.amalgamation.gradle.dependencies.remap.api.MappingTarget;
import io.github.astrarre.amalgamation.gradle.utils.AmalgIO;
import io.github.astrarre.amalgamation.gradle.utils.emptyfs.Err;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

import net.fabricmc.tinyremapper.TinyRemapper;

public class UnpickExtension implements TinyRemapper.Extension {
	public static final Field METHODS;
	public static final Field CLASS;

	static {
		try {
			METHODS = TargetMethods.class.getDeclaredField("methods");
			CLASS = TargetMethods.TargetMethod.class.getDeclaredField("declarator");
			METHODS.setAccessible(true);
			CLASS.setAccessible(true);
		} catch(ReflectiveOperationException e) {
			throw Err.rethrow(e);
		}
	}

	final List<MappingTarget> targets;
	Map<String, Method> methods;
	Map<String, ReplacementInstructionGenerator> constantGroups;

	public UnpickExtension(List<MappingTarget> targets) {
		this.targets = targets;
	}

	@Override
	public void attach(TinyRemapper.Builder builder) {
		ConcurrentHashMap<Integer, ConstantResolver> resolvers = new ConcurrentHashMap<>();
		Map<Integer, ConstantUninliner> uninliners = new ConcurrentHashMap<>();
		Map<String, String> namedToIntermediary = new ConcurrentHashMap<>();

		List<String> unidentifiedClasses = new Vector<>();
		builder.extraAnalyzeVisitor((mrjVersion, className, next) -> {
			unidentifiedClasses.add(className);
			return resolvers.computeIfAbsent(mrjVersion, j -> new ConstantResolver()).create(className, next);
		});

		builder.extraStateProcessor(env -> {
			for(ConstantResolver value : resolvers.values()) {
				value.populateMapped(env);
			}
			Iterator<String> iterator = unidentifiedClasses.iterator();
			while(iterator.hasNext()) {
				String name = iterator.next();
				iterator.remove();
				String map = env.getRemapper().map(name);
				if(!map.equals(name)) {
					namedToIntermediary.put(map, name);
				}
			}
			uninliners.computeIfAbsent(env.getMrjVersion(), integer -> {
				ConstantResolver resolver = resolvers.get(integer);
				return new ConstantUninliner(new TrConstantMapper(this, env, namedToIntermediary), resolver);
			});
		});

		builder.extraPostApplyVisitor((cls, next) -> {
			ConstantUninliner uninliner = uninliners.get(cls.getEnvironment().getMrjVersion());
			return new ClassVisitor(Opcodes.ASM9, next) {
				String name;

				@Override
				public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
					this.name = name;
					super.visit(version, access, name, signature, superName, interfaces);
				}

				@Override
				public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
					MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
					String internalName = this.name;
					return new MethodNode(Opcodes.ASM9, access, name, descriptor, signature, exceptions) {
						@Override
						public void visitEnd() {
							super.visitEnd();
							uninliner.transformMethod(internalName, this);
							this.accept(visitor);
						}
					};
				}
			};
		});
	}

	@SuppressWarnings("unchecked")
	public Map<String, Method> getMethods() {
		try {
			var methods = this.methods;
			if(methods == null) {
				synchronized(this) {
					methods = this.methods;
					if(methods != null) {
						return methods;
					}
					List<FileSystem> toClose = new ArrayList<>();
					List<InputStream> unpickDefinitions = new ArrayList<>();
					for(MappingTarget target : targets) {
						List<File> resolve = AmalgIO.resolve(target.project(), List.of(target.forward()));
						for(File file : resolve) {
							String name = file.getName();

							if(name.endsWith(".jar")) {
								FileSystem system = FileSystems.newFileSystem(file.toPath(), Map.of("create", "true"));
								Path path = system.getPath("extras/definitions.unpick");
								if(Files.exists(path)) {
									unpickDefinitions.add(Files.newInputStream(path));
								}
								toClose.add(system);
							} else if(name.endsWith(".unpick")) {
								unpickDefinitions.add(new FileInputStream(file));
							}

						}
					}
					var mapper = new DataDrivenConstantMapper(null, unpickDefinitions.toArray(InputStream[]::new)) {
						@Override
						public TargetMethods getTargetMethods() {
							return super.getTargetMethods();
						}

						public Map<String, ReplacementInstructionGenerator> generatorMap() {
							return this.constantGroups;
						}
					};
					TargetMethods targets = mapper.getTargetMethods();
					for(FileSystem system : toClose) {
						system.close();
					}

					Map<String, TargetMethods.TargetMethod> map = (Map) METHODS.get(targets);
					Map<String, Method> classMap = new HashMap<>(map.size());
					map.forEach((s, o) -> {
						try {
							Object o1 = CLASS.get(o);
							classMap.put(s, new Method(o, (String) o1));
						} catch(IllegalAccessException e) {
							throw Err.rethrow(e);
						}
					});
					this.constantGroups = mapper.generatorMap();
					this.methods = methods = classMap;
				}
			}
			return methods;
		} catch(IOException | IllegalAccessException e) {
			throw Err.rethrow(e);
		}
	}

	public Map<String, ReplacementInstructionGenerator> constantGroups() {
		this.getMethods();
		return this.constantGroups;
	}

	record Method(TargetMethods.TargetMethod mapping, String declarator) {}
}
