package io.github.astrarre.amalgamation.gradle.utils.json;

import java.util.ArrayList;
import java.util.List;
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

	abstract int finalIndex();

	public static Json parseValue(String buffer, int offset) {
		char c = buffer.charAt(offset);
		return switch(c) {
			case '{' -> new Obj(buffer, offset);
			case '\"' -> new Str(buffer, offset);
			case '[' -> new Arr(buffer, offset);
			case '0','1','2','3','4','5','6','7','8','9','-','.','N' -> new Num(buffer, offset);
			case 'n' -> new Null(buffer, offset);
			default -> throw new IllegalArgumentException("Foreign Character " + c + " " + buffer.substring(offset));
		};
	}

	public static final class Null extends Json {
		public Null(String buffer, int offset) {
			super(buffer, offset);
		}

		@Override
		int finalIndex() {
			return offset + 4;
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
			if(finalIndex == -1) {
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
						int dec = c-'0';
						acc *= mulAcc;
						val = val * mul + dec * acc;
						valL = valL * mulL + dec * accL;
					} else if(c == 'N') { // NaN
						val = Double.NaN;
						finalIndex = i;
						break;
					} else {
						finalIndex = i;
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

	public static final class Arr extends Json {
		final List<Json> objects = new ArrayList<>();
		boolean reachedEnd;
		int nextOff;

		public Arr(String buffer, int offset) {
			super(buffer, offset);
			this.nextOff = offset + 1;
		}

		@Override
		int finalIndex() {
			while(!this.reachedEnd) {
				this.get(this.objects.size());
			}
			return this.nextOff;
		}

		public Json get(int index) {
			List<Json> objects = this.objects;
			if(!(this.reachedEnd || index < objects.size())) {
				for(int i = objects.size(); i <= index; i++) {
					int valueIndex = this.readTo(this.nextOff, Json::isNotWhitespace);
					Json json = parseValue(this.buffer, valueIndex);
					objects.add(json);
					int terminatorLocation = this.readTo(json.getEnd(), Json::isNotWhitespace);
					this.nextOff = terminatorLocation + 1;
					if(this.buffer.charAt(terminatorLocation) == ']') {
						this.reachedEnd = true;
						break;
					} else {
						assertIs(terminatorLocation, ',');
					}
				}
			}
			return objects.get(index);
		}

		public boolean isReachedEnd() {
			return this.reachedEnd;
		}

		public int getLength() {
			this.getEnd();
			return this.objects.size();
		}
	}

	public static final class Obj extends Json {
		final TreeMap<String, Json> objects = new TreeMap<>();
		boolean reachedEnd;
		int nextOff;

		public Obj(String buffer, int offset) {
			super(buffer, offset);
			this.nextOff = offset + 1;
		}

		@Override
		int finalIndex() {
			while(!this.reachedEnd) {
				this.get(this.objects.size());
			}
			return this.nextOff;
		}

		public Json get(String key) {
			Json json = this.objects.get(key);
			if(json == null) {
				while(!this.reachedEnd) {
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

		public Map.Entry<String, Json> get(int index) {
			Map<String, Json> objects = this.objects;
			Map.Entry<String, Json> ret = null;
			if(!(this.reachedEnd || index < objects.size())) {
				for(int i = objects.size(); i <= index; i++) {
					int startKeyIndex = this.readTo(this.nextOff, Json::isNotWhitespace);
					Str key = (Str) parseValue(this.buffer, startKeyIndex);
					int sep = this.readTo(key.getEnd(), Json::isNotWhitespace);
					this.assertIs(sep, ':');
					int valueIndex = this.readTo(sep + 1, Json::isNotWhitespace);
					Json json = parseValue(this.buffer, valueIndex);
					int terminatorLocation = this.readTo(json.getEnd(), Json::isNotWhitespace);
					this.nextOff = terminatorLocation + 1;
					if(index == i) {
						ret = Map.entry(key.toString(), json);
					}

					if(this.buffer.charAt(terminatorLocation) == '}') {
						this.reachedEnd = true;
						break;
					}
				}
				return ret;
			} else {
				return Iterables.get(objects.entrySet(), index);
			}
		}

		public boolean isReachedEnd() {
			return this.reachedEnd;
		}

		public int getLength() {
			this.getEnd();
			return this.objects.size();
		}
	}

	public static final class Str extends Json {
		String value;
		int finalIndex;
		public Str(String buffer, int offset) {
			super(buffer, offset);
		}

		@Override
		int finalIndex() {
			this.getValue();
			return this.finalIndex;
		}

		public String getValue() {
			String value = this.value;
			if(value == null) {
				StringBuilder builder = null;
				int start = this.offset+1;
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
						this.finalIndex = i+1;
						this.value = value;
						break;
					}
				}
			}
			return value;
		}
	}

	protected int readTo(int start, CharPredicate predicate) {
		for(int i = start; i < buffer.length(); i++) {
			char at = buffer.charAt(i);
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

	@Override
	public String toString() {
		return this.buffer.substring(this.offset, this.getEnd());
	}
	
	static boolean isNotWhitespace(char ch) {
		return !Character.isWhitespace(ch);
	}
}
