package net.devtech.stacc;

import org.apache.commons.lang3.Validate;

public class ItemCountRenderHandler {
	private static ItemCountRenderHandler instance = new ItemCountRenderHandler();

	private static final char[] POWER = {
			'K',
			'M',
			'B',
			'T'
	};

	public String toConsiseString(int count) {
		int index = 0;
		if (count > 9999) {
			while (count / 1000 != 0) {
				count /= 1000;
				index++;
			}
		}

		if (index > 0) {
			return count + String.valueOf(POWER[index - 1]);
		} else {
			return String.valueOf(count);
		}
	}

	/**
	 * How big or small to render the item count text
	 * @return 1 == vanilla
	 */
	public float scale(String string) {
		if (string.length() > 3) {
			return .5f;
		} else if (string.length() == 3) {
			return .75f;
		}
		return 1f;
	}


	public static ItemCountRenderHandler getInstance() {
		return instance;
	}

	/**
	 * mods can override this class and set the instance.
	 * There can only really be one handler so it overrides any others.
	 */
	public static void setInstance(ItemCountRenderHandler instance) {
		Validate.notNull(instance, "instance cannot be null");
		ItemCountRenderHandler.instance = instance;
	}
}
