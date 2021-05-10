package tests;

import java.util.function.Consumer;

public abstract class ClassB implements Consumer<Integer> {
	@Override
	public void accept(Integer integer) {

	}

	public void run() {
	}

	public class Inner {

	}
}
