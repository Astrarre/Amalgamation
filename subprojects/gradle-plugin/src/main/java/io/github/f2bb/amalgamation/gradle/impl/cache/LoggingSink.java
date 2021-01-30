/*
 * Amalgamation
 * Copyright (C) 2021 Astrarre
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.github.f2bb.amalgamation.gradle.impl.cache;

import com.google.common.hash.PrimitiveSink;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class LoggingSink implements PrimitiveSink {

    private final PrimitiveSink delegate;
    private final PrintStream printStream;

    public LoggingSink(PrimitiveSink delegate, PrintStream printStream) {
        this.delegate = delegate;
        this.printStream = printStream;
    }

    @Override
    public PrimitiveSink putByte(byte b) {
        printStream.println("Byte: " + b);
        return delegate.putByte(b);
    }

    @Override
    public PrimitiveSink putBytes(byte @NotNull [] bytes) {
        printStream.println("Bytes: Count " + bytes.length);
        return delegate.putBytes(bytes);
    }

    @Override
    public PrimitiveSink putBytes(byte @NotNull [] bytes, int off, int len) {
        printStream.println("Bytes: Count " + (len - off));
        return delegate.putBytes(bytes, off, len);
    }

    @Override
    public PrimitiveSink putBytes(ByteBuffer bytes) {
        printStream.println("Bytes: Count " + bytes.remaining());
        return delegate.putBytes(bytes);
    }

    @Override
    public PrimitiveSink putShort(short s) {
        printStream.println("Short: " + s);
        return delegate.putShort(s);
    }

    @Override
    public PrimitiveSink putInt(int i) {
        printStream.println("Int: " + i);
        return delegate.putInt(i);
    }

    @Override
    public PrimitiveSink putLong(long l) {
        printStream.println("Long: " + l);
        return delegate.putLong(l);
    }

    @Override
    public PrimitiveSink putFloat(float f) {
        printStream.println("Float: " + f);
        return delegate.putFloat(f);
    }

    @Override
    public PrimitiveSink putDouble(double d) {
        printStream.println("Double: " + d);
        return delegate.putDouble(d);
    }

    @Override
    public PrimitiveSink putBoolean(boolean b) {
        printStream.println("Boolean: " + b);
        return delegate.putBoolean(b);
    }

    @Override
    public PrimitiveSink putChar(char c) {
        printStream.println("Char: " + c);
        return delegate.putChar(c);
    }

    @Override
    public PrimitiveSink putUnencodedChars(@NotNull CharSequence charSequence) {
        printStream.println("String: " + charSequence);
        return delegate.putUnencodedChars(charSequence);
    }

    @Override
    public PrimitiveSink putString(@NotNull CharSequence charSequence, @NotNull Charset charset) {
        printStream.println("String: " + charSequence);
        return delegate.putString(charSequence, charset);
    }
}
