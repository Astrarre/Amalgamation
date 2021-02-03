package io.github.f2bb.amalgamation.gradle.func;

import java.net.MalformedURLException;
import java.nio.file.Path;

import io.github.f2bb.amalgamation.gradle.impl.cache.Cache;

public interface CachedResolver {
	Path resolve(Cache cache, String output) throws MalformedURLException;
}
