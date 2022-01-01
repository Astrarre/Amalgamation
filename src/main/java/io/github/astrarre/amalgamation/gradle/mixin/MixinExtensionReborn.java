package io.github.astrarre.amalgamation.gradle.mixin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.gradle.api.logging.Logger;

import net.fabricmc.tinyremapper.ClassInstanceAccess;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.api.TrEnvironment;

public class MixinExtensionReborn implements TinyRemapper.Extension {
	final Logger logger;

	final Int2ObjectMap<MrjState> listeners = new Int2ObjectOpenHashMap<>();
	final Map<InputTag, List<MixinClass>> states = new ConcurrentHashMap<>();

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
				for(InputTag tag : ClassInstanceAccess.getInputTags(cls)) {
					states.computeIfAbsent(tag, t -> new ArrayList<>()).add(type);
				}
				return new SecondPassMixinVisitor(next, cls, type, this.logger);
			} else {
				return next;
			}
		});
	}

	public String generateRefmap(InputTag tag) {
		List<MixinClass> classes = this.states.get(tag);
		JsonObject refmap = new JsonObject();
		JsonObject mappings = new JsonObject();
		for(MixinClass type : classes) {
			if(type.mappings.isEmpty()) {
				continue;
			}
			JsonObject json = new JsonObject();
			type.mappings.forEach(json::addProperty);
			mappings.add(type.internalName, json);
		}
		refmap.add("mappings", mappings);
		JsonObject data = new JsonObject();
		data.add("named:intermediary", mappings);
		refmap.add("data", data);
		return refmap.toString();
	}

	record MrjState(List<Consumer<TrEnvironment>> listeners, Map<String, MixinClass> map) {
		public MrjState() {
			this(new ArrayList<>(), new HashMap<>());
		}
	}
}
