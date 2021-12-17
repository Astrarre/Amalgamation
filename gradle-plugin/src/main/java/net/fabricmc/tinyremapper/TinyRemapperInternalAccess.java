package net.fabricmc.tinyremapper;

import java.lang.reflect.Field;

import net.devtech.zipio.impl.util.U;

public class TinyRemapperInternalAccess {
	public static final Field DIRTY;
	public static final Field IS_INPUT;

	static {
		try {
			DIRTY = TinyRemapper.class.getDeclaredField("dirty");
			DIRTY.setAccessible(true);
			IS_INPUT = ClassInstance.class.getDeclaredField("isInput");
			IS_INPUT.setAccessible(true);
		} catch(NoSuchFieldException e) {
			throw U.rethrow(e);
		}
	}

	public static boolean isDirty(TinyRemapper remapper) {
		try {
			return DIRTY.getBoolean(remapper);
		} catch(IllegalAccessException e) {
			throw U.rethrow(e);
		}
	}

	public static void setDirty(TinyRemapper remapper, boolean dirty) {
		try {
			DIRTY.setBoolean(remapper, dirty);
		} catch(IllegalAccessException e) {
			throw U.rethrow(e);
		}
	}

	public static void setAllDirty(TinyRemapper remapper) {
		if (!isDirty(remapper)) {
			setDirty(remapper, true);

			for (TinyRemapper.MrjState state : remapper.mrjStates.values()) {
				state.dirty = true;
			}
		}
	}

	public static void unmarkAllAsInput(TinyRemapper remapper, boolean state) {
		try {
			for(TinyRemapper.MrjState value : remapper.mrjStates.values()) {
				for(ClassInstance instance : value.classes.values()) {
					if(instance.isInput ^ state) {
						IS_INPUT.setBoolean(instance, state);
					}
				}
			}
		} catch(ReflectiveOperationException e) {
			throw U.rethrow(e);
		}
	}
}
