package io.github.f2bb.amalgamation.util;

import java.io.IOException;
import java.io.InputStream;

public class ProcThread implements Runnable {
	private final Process process;
	private final InputStream out, err;
	public ProcThread(Process process) {
		this.process = process;
		this.out = process.getInputStream();
		this.err = process.getErrorStream();
	}

	private final byte[] buf = new byte[4096];

	@Override
	public void run() {
		try {
			while (this.process.isAlive()) {
				if(this.out.available() > 0) {
					int read = this.out.read(this.buf);
					if (read != -1) {
						System.out.write(this.buf, 0, read);
					}
				}

				if(this.err.available() > 0) {
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
