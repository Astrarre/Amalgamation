package tests;

import java.util.ArrayList;
import java.util.Vector;
import java.util.function.Function;

import io.github.f2bb.amalgamation.Interface;
import io.github.f2bb.amalgamation.Parent;
import io.github.f2bb.amalgamation.Platform;

@Interface (parent = Function.class,
		platforms = {
				@Platform ({
						"client"
				})
		})
@Parent(parent = Vector.class, platforms = @Platform({"client"}))
public class ClassA extends ArrayList implements Runnable, Function<Integer, String> {
	@Override
	public void run() {
	}

	@Override
	public String apply(Integer integer) {
		return null;
	}
}
