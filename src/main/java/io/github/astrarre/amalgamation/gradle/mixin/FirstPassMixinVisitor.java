package io.github.astrarre.amalgamation.gradle.mixin;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import com.google.common.collect.Iterables;
import org.gradle.api.logging.Logger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import net.fabricmc.tinyremapper.api.TrClass;
import net.fabricmc.tinyremapper.api.TrEnvironment;
import net.fabricmc.tinyremapper.api.TrMember;

public class FirstPassMixinVisitor extends ClassVisitor {
	final Logger logger;
	final MixinClass state;

	public FirstPassMixinVisitor(ClassVisitor visitor, Logger logger, int mrjVersion, String className, List<Consumer<TrEnvironment>> remapper) {
		super(Opcodes.ASM9, visitor);
		this.logger = logger;
		this.state = new MixinClass(remapper, mrjVersion, className);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if(descriptor.equals("Lorg/spongepowered/asm/mixin/Mixin;")) {
			this.state.isMixin = true;
			return new MixinAnnotationRemapper(this.api, super.visitAnnotation(descriptor, visible), this.state);
		} else if(descriptor.equals("Lorg/spongepowered/asm/mixin/Implements;")) {
			return new ImplementsAnnotationRemapper(this.api, super.visitAnnotation(descriptor, visible), this.state);
		}
		return super.visitAnnotation(descriptor, visible);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		FieldVisitor visitor = super.visitField(access, name, descriptor, signature, value);
		if(this.state.isMixin && this.state.remap) {
			if(Modifier.isPublic(access) && Modifier.isStatic(access)) {
				this.logger.error("Cannot have public static aliases in mixin " + this.state.internalName);
			}
			return new Field(visitor, name, descriptor);
		} else {
			return visitor;
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
		boolean isPrefixed = this.isPrefixed(name, descriptor);
		if(this.state.isMixin && this.state.remap && !isPrefixed) {
			if(Modifier.isPublic(access) && Modifier.isStatic(access)) {
				this.logger.error("Cannot have public static aliases in mixin " + this.state.internalName);
			}
			return new Method(visitor, name, descriptor);
		} else {
			return visitor;
		}
	}

	private boolean isPrefixed(String name, String descriptor) {
		boolean isPrefixed = false;
		if(this.state.isMixin && !this.state.prefixes.isEmpty()) {
			for(MixinClass.Implements prefix : this.state.prefixes) {
				if(name.startsWith(prefix.prefix())) {
					isPrefixed = true;
					FirstPassMixinVisitor.this.state.applyEnvironment(env -> {
						TrMember method = env.getMethod(FirstPassMixinVisitor.this.state.internalName, name, descriptor);
						String unprefixed = name.substring(prefix.prefix().length());
						String mappedName = env.getRemapper().mapMethodName(prefix.interfaceInternalName(), unprefixed, descriptor);
						String prefixed = prefix.prefix() + mappedName;
						if(!mappedName.equals(unprefixed)) {
							env.propagate(method, prefixed);
						}
					});
				}
			}
		}
		return isPrefixed;
	}

	void deriveFromName(TrEnvironment env,
			Iterable<String> aliases,
			String methodName,
			String methodDesc,
			String targetDesc,
			UnaryOperator<String> deriveName,
			boolean isMethod,
			boolean forceAlias) {
		TrClass currentClass = env.getClass(state.internalName);
		boolean propagate = true;
		for(String target : state.targets) {
			for(String alias : aliases) {
				TrMember method = currentClass.getMethod(methodName, methodDesc);
				String name;
				if(isMethod) {
					name = env.getRemapper().mapMethodName(target, alias, targetDesc);
				} else {
					name = env.getRemapper().mapFieldName(target, alias, targetDesc);
				}
				if(!name.equals(alias)) {
					if(propagate) {
						env.propagate(method, deriveName.apply(name));
					}
					if(!propagate || forceAlias) {
						state.aliases.put(new MixinClass.Member(methodName, methodDesc), name);
					}
					propagate = false;
				}
			}
		}
	}

	class Field extends FieldVisitor {
		final String name, desc;

		public Field(FieldVisitor methodVisitor, String name, String desc) {
			super(FirstPassMixinVisitor.this.api, methodVisitor);
			this.name = name;
			this.desc = desc;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			AnnotationVisitor visitor = super.visitAnnotation(descriptor, visible);
			switch(descriptor) {
				case "Lorg/spongepowered/asm/mixin/Shadow;" -> new ShadowVisitor(visitor, false, name, desc);
				case "Lorg/spongepowered/asm/mixin/gen/Accessor;" -> new InvokerAccessorVisitor(visitor, this.name, this.desc, false);
			}
			return super.visitAnnotation(descriptor, visible);
		}
	}

	class Method extends MethodVisitor {
		final String name, desc;

		public Method(MethodVisitor methodVisitor, String name, String desc) {
			super(FirstPassMixinVisitor.this.api, methodVisitor);
			this.name = name;
			this.desc = desc;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			AnnotationVisitor visitor = super.visitAnnotation(descriptor, visible);
			return switch(descriptor) {
				case "Lorg/spongepowered/asm/mixin/Overwrite;" -> new OverwriteVisitor(visitor, name, desc);
				case "Lorg/spongepowered/asm/mixin/Shadow;" -> new ShadowVisitor(visitor, true, this.name, this.desc);
				case "Lorg/spongepowered/asm/mixin/gen/Invoker;" -> new InvokerAccessorVisitor(visitor, this.name, this.desc, true);
				default -> visitor;
			};
		}
	}

	class InvokerAccessorVisitor extends AnnotationVisitor {
		final String memberName, memberDesc;
		final boolean isMethod;
		final String prefix;
		final String desc;
		String value;
		boolean remap = true;

		public InvokerAccessorVisitor(AnnotationVisitor annotationVisitor, String name, String methodDesc, boolean method) {
			super(FirstPassMixinVisitor.this.api, annotationVisitor);
			this.memberDesc = methodDesc;
			if(method) {
				desc = methodDesc;
			} else {
				Type type = Type.getMethodType(methodDesc);
				Type[] types = type.getArgumentTypes();
				if(types.length == 0) {
					// getter
					desc = type.getReturnType().getDescriptor();
				} else {
					if(types.length != 1) {
						logger.warn(state.internalName + "#" + name + " has invalid descriptor for setter!");
					}
					desc = types[0].getDescriptor();
				}
			}
			this.isMethod = method;
			this.memberName = name;
			if(state.targets.size() > 1) {
				logger.warn("More than one target for " + state.internalName + " in Invoker/Accessor, they do not have alias arrays and thus will " +
				            "mald in the event there are multiple intermediaries for the same member name");
			}
			String parsed;
			if(method) {
				if(name.startsWith("invoke")) {
					parsed = name.substring(4);
					prefix = "invoke";
				} else if(name.startsWith("call")) {
					parsed = name.substring(3);
					prefix = "call";
				} else {
					this.value = null;
					prefix = null;
					return;
				}
			} else {
				if(name.startsWith("is")) {
					parsed = name.substring(2);
					prefix = "is";
				} else if(name.startsWith("set")) {
					parsed = name.substring(3);
					prefix = "set";
				} else if(name.startsWith("get")) {
					parsed = name.substring(3);
					prefix = "get";
				} else {
					this.value = null;
					prefix = null;
					return;
				}
			}
			this.value = parsed.substring(0, 1).toLowerCase(Locale.ROOT) + parsed.substring(1);
		}

		@Override
		public void visit(String name, Object value) {
			super.visit(name, value);
			if(name.equals("value")) {
				this.value = (String) value;
			} else if(name.equals("remap")) {
				this.remap = (Boolean) value;
			}
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			if(value == null) {
				if(isMethod) {
					logger.warn(state.internalName + "#" + this.memberName + " does not start with call/invoke!");
				} else {
					logger.warn(state.internalName + "#" + this.memberName + " does not start with is/set/get!");
				}
				return;
			}

			if(this.remap) {
				FirstPassMixinVisitor.this.state.applyEnvironment(env -> {
					deriveFromName(env, List.of(value), this.memberName, this.memberDesc, this.desc, name -> {
						return this.prefix + name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
					}, true, true);
				});
			}
		}
	}

	class OverwriteVisitor extends AnnotationVisitor {
		final List<String> aliases;
		final String name, desc;
		boolean remap;

		public OverwriteVisitor(AnnotationVisitor visitor, String name, String desc) {
			super(FirstPassMixinVisitor.this.api, visitor);
			this.name = name;
			this.desc = desc;
			aliases = new ArrayList<>();
			remap = true;
		}

		@Override
		public void visit(String name, Object value) {
			if(name.equals("remap")) {
				this.remap = (Boolean) value;
			}
			super.visit(name, value);
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			AnnotationVisitor visitor = super.visitArray(name);
			if(name.equals("aliases")) {
				return new AliasCollector(visitor, aliases);
			} else {
				return visitor;
			}
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			FirstPassMixinVisitor.this.state.applyEnvironment(env -> {
				deriveFromName(
						env,
						Iterables.concat(List.of(this.name), this.aliases),
						this.name,
						this.desc,
						this.desc,
						UnaryOperator.identity(),
						true,
						false
				);
			});
		}
	}

	class ShadowVisitor extends AnnotationVisitor {
		final List<String> aliases = new ArrayList<>();
		final boolean isMethod;
		final String name, desc;
		boolean remap;
		String prefix;

		public ShadowVisitor(AnnotationVisitor visitor, boolean method, String name, String desc) {
			super(FirstPassMixinVisitor.this.api, visitor);
			this.isMethod = method;
			this.name = name;
			this.desc = desc;
			this.remap = true;
		}

		@Override
		public void visit(String name, Object value) {
			if(name.equals("remap")) {
				this.remap = (Boolean) value;
			} else if(name.equals("prefix")) {
				this.prefix = (String) value;
			}
			super.visit(name, value);
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			AnnotationVisitor visitor = super.visitArray(name);
			if(name.equals("aliases")) {
				return new AliasCollector(visitor, aliases);
			} else {
				return visitor;
			}
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			if(this.remap) {
				String prefixedName = this.name;
				String realPrefix = this.prefix == null ? "shadow$" : this.prefix;
				boolean hasPrefix = prefixedName.startsWith(realPrefix);
				String trueName;
				if(hasPrefix) {
					trueName = prefixedName.substring(realPrefix.length());
				} else {
					trueName = prefixedName;
				}

				FirstPassMixinVisitor.this.state.applyEnvironment(env -> {
					FirstPassMixinVisitor.this.deriveFromName(env,
							Iterables.concat(List.of(trueName), this.aliases),
							this.name,
							this.desc,
							this.desc,
							name -> hasPrefix ? realPrefix + name : name,
							isMethod,
							false
					);
				});
			}
		}
	}

	class AliasCollector extends AnnotationVisitor {
		final List<String> aliases;

		public AliasCollector(AnnotationVisitor visitor, List<String> aliases) {
			super(FirstPassMixinVisitor.this.api, visitor);
			this.aliases = aliases;
		}

		@Override
		public void visit(String name, Object value) {
			super.visit(name, value);
			aliases.add((String) value);
		}
	}
}
