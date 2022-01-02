package io.github.astrarre.amalgamation.gradle.mixin;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.gradle.api.logging.Logger;

import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.api.TrEnvironment;

public class MixinExtensionReborn implements TinyRemapper.Extension {
	final Logger logger;

	final Int2ObjectMap<MrjState> listeners = new Int2ObjectOpenHashMap<>();

	public MixinExtensionReborn(Logger logger) {this.logger = logger;}

	@Override
	public void attach(TinyRemapper.Builder builder) {
		Lock lock = new ReentrantLock();
		builder.extraAnalyzeVisitor((mrjVersion, className, next) -> {
			MrjState state;
			try {
				lock.lock();
				state = listeners.computeIfAbsent(mrjVersion, m -> new MrjState());
			} finally {
				lock.unlock();
			}

			FirstPassMixinVisitor visitor = new FirstPassMixinVisitor(next, logger, mrjVersion, className, state.listeners);
			state.map.put(className, visitor.state);
			return visitor;
		});

		builder.extraStateProcessor(env -> {
			MrjState state = this.listeners.get(env.getMrjVersion());
			if(state != null) {
				for(Consumer<TrEnvironment> listener : state.listeners) {
					listener.accept(env);
				}
				state.listeners.clear();
			}
		});

		builder.extraPreApplyVisitor((cls, next) -> {
			MrjState state;
			try {
				lock.lock();
				state = listeners.get(cls.getEnvironment().getMrjVersion());
			} finally {
				lock.unlock();
			}
			MixinClass type = state.map.get(cls.getName());
			if(type.isMixin) {
				return new SecondPassMixinVisitor(next, cls, type, this.logger);
			} else {
				return next;
			}
		});
	}

	record MrjState(List<Consumer<TrEnvironment>> listeners, Map<String, MixinClass> map) {
		public MrjState() {
			this(new Vector<>(), new ConcurrentHashMap<>());
		}
	}
}
