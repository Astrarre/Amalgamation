package io.github.astrarre.amalgamation.gradle.utils.json;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.chars.CharPredicate;

public abstract class Json {
	final String buffer;
	final int offset;
	int finalIndex = -1;

	public Json(String buffer, int offset) {
		this.buffer = buffer;
		this.offset = offset;
	}

	public int getEnd() {
		if(this.finalIndex == -1) {
			this.finalIndex = this.finalIndex();
		}
		return this.finalIndex;
	}

	public static Json parseValue(String buffer, int offset) {
		char c = buffer.charAt(offset);
		return switch(c) {
			case '{' -> new Obj(buffer, offset);
			case '\"' -> new Str(buffer, offset);
			case '[' -> new List(buffer, offset);
			case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '.', 'N' -> new Num(buffer, offset);
			case 'n' -> new Null(buffer, offset);
			case 't' -> new Bool(buffer, offset, true);
			case 'f' -> new Bool(buffer, offset, false);
			default -> throw new IllegalArgumentException("Foreign Character " + c + " " + buffer.substring(offset));
		};
	}

	@Override
	public String toString() {
		return this.buffer.substring(this.offset, this.getEnd());
	}

	static boolean isNotWhitespace(char ch) {
		return !Character.isWhitespace(ch);
	}

	abstract int finalIndex();

	protected int readTo(int start, CharPredicate predicate) {
		for(int i = start; i < this.buffer.length(); i++) {
			char at = this.buffer.charAt(i);
			if(predicate.test(at)) {
				return i;
			}
		}
		return -1;
	}

	protected void assertIs(int index, char predicate) {
		char c = this.buffer.charAt(index);
		if(c != predicate) {
			throw new IllegalStateException("Expected " + predicate + " found " + c);
		}
	}

	public static final class Null extends Json {
		public Null(String buffer, int offset) {
			super(buffer, offset);
		}

		@Override
		int finalIndex() {
			return this.offset + 4;
		}
	}

	public static final class Bool extends Json {
		final boolean value;

		public Bool(String buffer, int offset, boolean value) {
			super(buffer, offset);
			this.value = value;
		}

		public boolean getValue() {
			return this.value;
		}

		@Override
		int finalIndex() {
			return (this.value ? 4 : 5) + this.offset;
		}
	}

	public static final class Num extends Json {
		double value = Double.NEGATIVE_INFINITY; // json spec no support it
		long longValue;
		int finalIndex = -1;

		public Num(String buffer, int offset) {
			super(buffer, offset);
		}

		public long getLongValue() {
			this.getValue();
			return this.longValue;
		}

		public double getValue() {
			double value = this.value;
			if(this.finalIndex == -1) {
				long valL = 0, mulL = 10, accL = 1;
				double val = 0, mul = 10, acc = 1, mulAcc = 1;
				for(int i = this.offset; i < this.buffer.length(); i++) {
					char c = this.buffer.charAt(i);
					if(c == '.') {
						acc = Math.signum(acc);
						mulAcc = .1;
						accL = 0;
						mul = 1;
						mulL = 1;
					} else if(c == '-') {
						acc = -1;
						accL = -1;
					} else if(Character.isDigit(c)) {
						int dec = c - '0';
						acc *= mulAcc;
						val = val * mul + dec * acc;
						valL = valL * mulL + dec * accL;
					} else if(c == 'N') { // NaN
						val = Double.NaN;
						this.finalIndex = i;
						break;
					} else {
						this.finalIndex = i;
						break;
					}
				}
				this.longValue = valL;
				this.value = value = val;
			}
			return value;
		}

		@Override
		int finalIndex() {
			this.getValue();
			return this.finalIndex;
		}
	}

	protected static abstract class LazyMultiple<T> extends Json {
		boolean reachedEnd;
		int nextOff;
		Json lastElement;

		public LazyMultiple(String buffer, int offset) {
			super(buffer, offset);
			this.nextOff = offset + 1;
		}

		public int currentlyParsed() {
			return this.size0();
		}

		public T get(int index) {
			int size = this.size0();
			if(!this.hasReachedEnd() && index >= size) {
				T t = null;
				for(int i = size; i <= index; i++) {
					t = this.get0(i);
				}
				return t;
			}
			return this.getCollect0(index);
		}

		public int getLength() {
			this.getEnd();
			return this.size0();
		}

		public boolean hasReachedEnd() {
			Json json = this.lastElement;
			if(json == null) {
				return this.reachedEnd;
			}
			int terminatorLocation = this.readTo(json.getEnd(), Json::isNotWhitespace);
			this.nextOff = terminatorLocation + 1;
			if(this.buffer.charAt(terminatorLocation) == this.terminator()) {
				this.reachedEnd = true;
				this.lastElement = null;
				return true;
			} else {
				this.assertIs(terminatorLocation, ',');
				return false;
			}
		}

		@Override
		int finalIndex() {
			while(!this.hasReachedEnd()) {
				this.get(this.size0());
			}
			return this.nextOff;
		}

		abstract T get0(int index);

		abstract T getCollect0(int index);

		abstract int size0();

		abstract char terminator();
	}

	public static final class List extends LazyMultiple<Json> {
		final java.util.List<Json> objects = new ArrayList<>();

		public List(String buffer, int offset) {
			super(buffer, offset);
		}

		@Override
		public Json get0(int index) {
			int valueIndex = this.readTo(this.nextOff, Json::isNotWhitespace);
			Json json = parseValue(this.buffer, valueIndex);
			this.lastElement = json;
			this.objects.add(json);
			return json;
		}

		@Override
		Json getCollect0(int index) {return this.objects.get(index);}

		@Override
		int size0() {return this.objects.size();}

		@Override
		char terminator() {return ']';}

		public java.util.List<Json> asList() {
			return new ListImpl();
		}

		private class ListImpl extends AbstractList<Json> {
			@Override
			public Json get(int index) {
				return List.this.get(index);
			}

			@Override
			public int size() {
				return List.this.getLength();
			}
		}
	}

	public static final class Obj extends LazyMultiple<Map.Entry<String, Json>> {
		final TreeMap<String, Json> objects = new TreeMap<>();

		public Obj(String buffer, int offset) {
			super(buffer, offset);
		}

		public Json get(String key) {
			Json json = this.objects.get(key);
			if(json == null) {
				while(!this.hasReachedEnd()) {
					Map.Entry<String, Json> entry = this.get(this.objects.size());
					if(entry.getKey().equals(key)) {
						return entry.getValue();
					}
				}
				return null;
			} else {
				return json;
			}
		}

		public boolean hasParsed(String key) {
			return this.objects.containsKey(key);
		}

		public String getString(String key) {
			return ((Str) this.get(key)).getValue();
		}

		public Obj getObj(String key) {
			return (Obj) this.get(key);
		}

		public List getList(String key) {
			return (List) this.get(key);
		}

		@Override
		public Map.Entry<String, Json> get0(int index) {
			Map<String, Json> objects = this.objects;
			int startKeyIndex = this.readTo(this.nextOff, Json::isNotWhitespace);
			Str key = (Str) parseValue(this.buffer, startKeyIndex);
			int sep = this.readTo(key.getEnd(), Json::isNotWhitespace);
			this.assertIs(sep, ':');
			int valueIndex = this.readTo(sep + 1, Json::isNotWhitespace);
			Json json = this.lastElement = parseValue(this.buffer, valueIndex);
			String value = key.getValue();
			objects.put(value, json);
			return Map.entry(value, json);
		}

		@Override Map.Entry<String, Json> getCollect0(int index) {return Iterables.get(this.objects.entrySet(), index);}
		@Override int size0() {return this.objects.size();}
		@Override char terminator() {return '}';}

		public Map<String, Json> asMap() {
			this.getLength();
			return Collections.unmodifiableMap(this.objects);
		}
	}

	public static final class Str extends Json {
		String value;
		int finalIndex;

		public Str(String buffer, int offset) {
			super(buffer, offset);
		}

		public String getValue() {
			String value = this.value;
			if(value == null) {
				StringBuilder builder = null;
				int start = this.offset + 1;
				String buffer = this.buffer;
				for(int i = start; i < buffer.length(); i++) {
					char c = buffer.charAt(i);
					if(buffer.charAt(i) == '\\') {
						if(builder == null) {
							builder = new StringBuilder();
						}
						builder.append(this.buffer, start, i);
						start = ++i;
					} else if(c == '\"') {
						if(builder == null) { // no escaped characters
							value = buffer.substring(start, i);
						} else {
							builder.append(this.buffer, start, i);
							value = builder.toString();
						}
						this.finalIndex = i + 1;
						this.value = value;
						break;
					}
				}
			}
			return value;
		}

		@Override
		int finalIndex() {
			this.getValue();
			return this.finalIndex;
		}
	}
}
