package io.github.astrarre.amalgamation.gradle.dependencies.util;

import java.util.List;

import com.google.common.collect.ForwardingList;

public class DependencyList extends ForwardingList<Object> {
	final List<Object> dependencies;
	Runnable callback;

	public DependencyList(List<Object> dependencies, Runnable callback) {
		this.dependencies = dependencies;
		this.callback = callback;
	}

	@Override
	protected synchronized List<Object> delegate() {
		if(this.callback != null) {
			this.callback.run();
			this.callback = null;
		}
		return this.dependencies;
	}
}
