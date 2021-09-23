package io.github.astrarre.amalgamation.gradle.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

import io.github.astrarre.amalgamation.api.Access;
import io.github.astrarre.amalgamation.api.Displace;
import io.github.astrarre.amalgamation.api.Interface;
import io.github.astrarre.amalgamation.api.Parent;
import io.github.astrarre.amalgamation.api.Platform;
import org.objectweb.asm.Type;

public class Constants {
	public static final ExecutorService SERVICE = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), pool -> {
		ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
		thread.setDaemon(true);
		return thread;
	}, null, true);
}
