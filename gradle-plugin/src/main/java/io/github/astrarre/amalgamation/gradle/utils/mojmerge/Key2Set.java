package io.github.astrarre.amalgamation.gradle.utils.mojmerge;

import static it.unimi.dsi.fastutil.HashCommon.arraySize;

import java.util.Objects;

import it.unimi.dsi.fastutil.HashCommon;

public final class Key2Set<K1, K2> {
	private final float loadFactor;
	public Object[] key1, key2;
	private int mask;
	public int size;
	private int maxFill;

	public Key2Set(int size) {
		this(.5f, size);
	}

	public Key2Set(float factor, int size) {
		this(HashCommon.arraySize(size, factor), factor);
	}

	private Key2Set(int n, float factor) {
		this.loadFactor = factor;
		this.resize(n);
	}

	private void resize(int n) {
		this.mask = n - 1;
		this.maxFill = HashCommon.maxFill(n, this.loadFactor);
		this.key1 = new Object[n];
		this.key2 = new Object[n];
	}

	public static int hash(Object a, Object b) {
		int result = Objects.hashCode(a);
		result = 31 * result + Objects.hashCode(b);
		return result;
	}

	public boolean put(K1 k1, K2 k2) {
		final int pos = this.find(k1, k2);
		if(pos < 0) {
			int insertPos = -pos - 1;
			this.key1[insertPos] = k1;
			this.key2[insertPos] = k2;

			if(this.size++ >= this.maxFill) {
				int size = arraySize(this.size + 1, this.loadFactor);
				Object[] key1 = this.key1, key2 = this.key2;
				this.resize(size);
				final int mask1 = size - 1; // Note that this is used by the hashing macro
				int pos1;
				for(int i = key2.length - 1; i >= 0; i--) {
					if(this.hasEntry(pos1 = HashCommon.mix(hash(key1[i], key2[i])) & mask1)) {
						while(this.hasEntry(pos1 = pos1 + 1 & mask1)) {
						}
					}

					this.key1[pos1] = key1[i];
					this.key2[pos1] = key2[i];
				}
			}

			return true;
		} else {
			return false;
		}
	}

	public boolean remove(K1 k1, K2 k2) {
		int mask = this.mask;
		for(int pos = HashCommon.mix(hash(k1, k2)) & mask; ; pos = pos + 1 & mask) {
			if(!this.hasEntry(pos)) {
				return false;
			}
			if(this.isKeyEqualToIncoming(k1, k2, pos)) {
				this.size--;
				this.shiftKeys(pos);
				return true;
			}
		}
	}

	public boolean contains(K1 k1, K2 k2) {
		int mask = this.mask;
		for(int pos = HashCommon.mix(hash(k1, k2)) & mask; ; pos = pos + 1 & mask) {
			if(!this.hasEntry(pos)) {
				return false;
			}
			if(this.isKeyEqualToIncoming(k1, k2, pos)) {
				return true;
			}
		}
	}

	private void shiftKeys(int pos) {
		// Shift entries with the same hash.
		while(true) {
			int last = pos;
			pos = last + 1 & this.mask;
			int currentIndex = pos;

			while(true) {
				if(!this.hasEntry(pos)) {
					this.key1[last] = null;
					this.key2[last] = null;
					return;
				}
				int slot = HashCommon.mix(hash(this.key1[pos], this.key2[pos])) & this.mask;
				if(last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) {
					break;
				}
				pos = (pos + 1) & this.mask;
			}
			this.key1[last] = this.key1[currentIndex];
			this.key2[last] = this.key2[currentIndex];
		}
	}

	private int find(K1 k1, K2 k2) {
		int mask = this.mask;
		for(int pos = HashCommon.mix(hash(k1, k2)) & mask; ; pos = pos + 1 & mask) {
			if(!this.hasEntry(pos)) {
				return -(pos + 1);
			}
			if(this.isKeyEqualToIncoming(k1, k2, pos)) {
				return pos;
			}
		}
	}

	private boolean hasEntry(int index) {
		return this.key1[index] != null || this.key2[index] != null;
	}

	private boolean isKeyEqualToIncoming(K1 k1, K2 k2, int index) {
		return Objects.equals(k1, this.key1[index]) && Objects.equals(k2, this.key2[index]);
	}
}