package io.github.astrarre.amalgamation.gradle.utils;

import java.util.Collection;
import java.util.function.Predicate;

import com.google.common.collect.Iterables;

public class CollectionUtil {
	public static <T> Iterable<T> filt(Iterable<T> incoming, Collection<T> excess, Predicate<T> test) {
		for (T t : incoming) {
			if(test.test(t)) {
				excess.add(t);
			}
		}
		return Iterables.filter(incoming, input -> !test.test(input));
	}
}
