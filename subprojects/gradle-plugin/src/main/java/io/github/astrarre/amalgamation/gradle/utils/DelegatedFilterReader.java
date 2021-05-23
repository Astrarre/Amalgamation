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

package io.github.astrarre.amalgamation.gradle.utils;

import org.jetbrains.annotations.NotNull;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

public class DelegatedFilterReader extends FilterReader {

    public Reader data;

    public DelegatedFilterReader(Reader in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        this.lock = this.in = this.data;
        return super.read();
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        this.lock = this.in = this.data;
        return super.read(cbuf, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        this.lock = this.in = this.data;
        return super.skip(n);
    }

    @Override
    public boolean ready() throws IOException {
        this.lock = this.in = this.data;
        return super.ready();
    }

    @Override
    public boolean markSupported() {
        this.lock = this.in = this.data;
        return super.markSupported();
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        this.lock = this.in = this.data;
        super.mark(readAheadLimit);
    }

    @Override
    public void reset() throws IOException {
        this.lock = this.in = this.data;
        super.reset();
    }

    @Override
    public void close() throws IOException {
        this.lock = this.in = this.data;
        super.close();
    }

    @Override
    public int read(@NotNull CharBuffer target) throws IOException {
        this.lock = this.in = this.data;
        return super.read(target);
    }

    @Override
    public int read(char[] cbuf) throws IOException {
        this.lock = this.in = this.data;
        return super.read(cbuf);
    }
}
