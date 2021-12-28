package io.github.astrarre.amalgamation.gradle.ide;

import org.gradle.api.Task;

public abstract class NamedTaskConverter<T extends Task> extends TaskConverter<T> {
	public String customName;
	public NamedTaskConverter(T task) {
		super(task);
		this.setCustomName(task.getName());
	}

	public String getCustomName() {
		return this.customName;
	}

	public void setCustomName(String customName) {
		this.customName = customName;
	}
}
