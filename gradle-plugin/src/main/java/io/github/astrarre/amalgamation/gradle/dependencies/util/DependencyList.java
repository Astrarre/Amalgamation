package io.github.astrarre.amalgamation.gradle.dependencies.util;

import java.util.List;

import com.google.common.collect.ForwardingList;
import org.gradle.api.artifacts.SelfResolvingDependency;

public class DependencyList extends ForwardingList<Object> {
	final List<Object> dependencies;
	public SelfResolvingDependency callback;

	public DependencyList(List<Object> dependencies, SelfResolvingDependency callback) {
		this.dependencies = dependencies;
		this.callback = callback;
	}

	@Override
	protected synchronized List<Object> delegate() {
		if(this.callback != null) {
			this.callback.resolve();
			this.callback = null;
		}
		return this.dependencies;
	}
}
