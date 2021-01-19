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

package io.github.f2bb.amalgamation.platform.util;

import java.io.IOException;
import java.io.InputStream;

public class ProcThread implements Runnable {
    private final Process process;
    private final InputStream out, err;
    private final byte[] buf = new byte[4096];

    public ProcThread(Process process) {
        this.process = process;
        this.out = process.getInputStream();
        this.err = process.getErrorStream();
    }

    @Override
    public void run() {
        try {
            while (this.process.isAlive()) {
                if (this.out.available() > 0) {
                    int read = this.out.read(this.buf);
                    if (read != -1) {
                        System.out.write(this.buf, 0, read);
                    }
                }

                if (this.err.available() > 0) {
                    int read = this.err.read(this.buf);
                    if (read != -1) {
                        System.err.write(this.buf, 0, read);
                    }
                }

                Thread.yield();
                Thread.sleep(500);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
