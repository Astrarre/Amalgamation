package io.github.astrarre.amalgamation.gradle.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.gradle.api.logging.Logger;
import org.jetbrains.annotations.Nullable;

public class DownloadUtil {
	public static Result read(URL url, @Nullable String etag, long currentLastModifyDate, Logger logger, boolean offline, boolean compressed) throws IOException {
		Clock clock = new Clock("Validating/Downloading " + url + " cache took %dms", logger);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		// If the output already exists we'll use it's last modified time
		if (currentLastModifyDate != -1) {
			if(offline) {
				clock.close();
				return null;
			}
			connection.setIfModifiedSince(currentLastModifyDate);
		}

		if (etag != null) {
			connection.setRequestProperty("If-None-Match", etag);
		}


		if(compressed) {
			// We want to download gzip compressed stuff
			connection.setRequestProperty("Accept-Encoding", "gzip"); // todo maybe use brotili idk
		}

		// Try make the connection, it will hang here if the connection is bad
		connection.connect();

		int code = connection.getResponseCode();

		if ((code < 200 || code > 299) && code != HttpURLConnection.HTTP_NOT_MODIFIED) {
			//Didn't get what we expected
			clock.close();
			Result result = new Result(null, -1,clock, null);
			result.error = new IOException(connection.getResponseMessage() + " for " + url);
			return result;
		}

		long modifyTime = connection.getHeaderFieldDate("Last-Modified", -1);

		String nEtag = connection.getHeaderField("ETag");
		if (currentLastModifyDate != -1 && (code == HttpURLConnection.HTTP_NOT_MODIFIED || modifyTime > 0 && currentLastModifyDate >= modifyTime)) {
			if(logger != null) logger.lifecycle("'{}' Not Modified, skipping.", url);
			clock.close();
			return new Result(null, currentLastModifyDate, clock, nEtag); // What we've got is already fine
		}

		long contentLength = connection.getContentLengthLong();

		if (contentLength >= 0) {
			if(logger != null) logger.lifecycle("'{}' Changed, downloading {}", url, toNiceSize(contentLength));
		}

		try { // Try download to the output
			InputStream stream = connection.getInputStream();
			return new Result(stream, modifyTime, clock, nEtag);
		} catch (IOException e) {
			clock.close();
			Result result = new Result(null, -1,clock, null);
			result.error = e;
			return result;
		}
	}


	public static class Result implements AutoCloseable {
		public final InputStream stream;
		public final long lastModifyDate;
		public final Clock clock;
		public final String etag;
		public IOException error;

		public Result(InputStream stream, long date, Clock clock, String etag) {
			this.stream = stream;
			this.lastModifyDate = date;
			this.clock = clock;
			this.etag = etag;
		}

		@Override
		public void close() {
			if(clock != null) {
				clock.close();
			}
			if(stream != null) {
				try {
					stream.close();
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	/**
	 * Format the given number of bytes as a more human readable string.
	 *
	 * @param bytes The number of bytes
	 * @return The given number of bytes formatted to kilobytes, megabytes or gigabytes if appropriate
	 */
	public static String toNiceSize(long bytes) {
		if(bytes < 1024) {
			return bytes + " B";
		} else if(bytes < 1_048_576) {
			return bytes / 1024 + " KB";
		} else if(bytes < 1_073_741_824) {
			return String.format("%.2f MB", bytes / (1_048_576.0));
		} else {
			return String.format("%.2f GB", bytes / (1_073_741_824.0));
		}
	}
}
