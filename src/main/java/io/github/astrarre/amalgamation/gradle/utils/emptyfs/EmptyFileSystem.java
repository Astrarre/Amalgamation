package io.github.astrarre.amalgamation.gradle.utils.emptyfs;


import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jetbrains.annotations.NotNull;

public class EmptyFileSystem extends FileSystem {
	public final EmptyPath root = new EmptyPath(this, "/");
	final EmptyFileSystemProvider provider;
	final List<Path> roots = List.of(this.root);
	final List<FileStore> stores = List.of(new EmptyFileStore());
	final URI uri;
	boolean isOpen;
	
	public EmptyFileSystem(EmptyFileSystemProvider provider, URI uri) {
		this.provider = provider;
		this.uri = uri;
	}
	
	@Override
	public FileSystemProvider provider() {
		return this.provider;
	}
	
	@Override
	public void close() throws IOException {
		this.provider.system.remove(this.uri, this);
		this.isOpen = false;
	}
	
	@Override
	public boolean isOpen() {
		return this.isOpen;
	}
	
	@Override
	public boolean isReadOnly() {
		return true;
	}
	
	@Override
	public String getSeparator() {
		return "/";
	}
	
	@Override
	public Iterable<Path> getRootDirectories() {
		return this.roots;
	}
	
	@Override
	public Iterable<FileStore> getFileStores() {
		return this.stores;
	}
	
	@Override
	public Set<String> supportedFileAttributeViews() {
		return Set.of();
	}
	
	@NotNull
	@Override
	public Path getPath(@NotNull String first, @NotNull String... more) {
		String str = first;
		if(more.length != 0) {
			StringBuilder builder = new StringBuilder(first);
			for(String s : more) {
				builder.append('/').append(s);
			}
			str = builder.toString();
		}
		return new EmptyPath(this, str);
	}
	
	private static final String GLOB_SYNTAX = "glob";
	private static final String REGEX_SYNTAX = "regex";
	@Override
	public PathMatcher getPathMatcher(String syntaxAndInput) {
		int pos = syntaxAndInput.indexOf(':');
		if (pos <= 0 || pos == syntaxAndInput.length()) {
			throw new IllegalArgumentException();
		}
		String syntax = syntaxAndInput.substring(0, pos);
		String input = syntaxAndInput.substring(pos + 1);
		String expr;
		if (syntax.equalsIgnoreCase(GLOB_SYNTAX)) {
			expr = toRegexPattern(input);
		} else {
			if (syntax.equalsIgnoreCase(REGEX_SYNTAX)) {
				expr = input;
			} else {
				throw new UnsupportedOperationException("Syntax '" + syntax +
				                                        "' not recognized");
			}
		}
		// return matcher
		final Pattern pattern = Pattern.compile(expr);
		return (path)->pattern.matcher(path.toString()).matches();
	}
	
	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public WatchService newWatchService() throws IOException {
		return new WatchService() {
			@Override
			public void close() throws IOException {
			}
			
			@Override
			public WatchKey poll() {
				return null;
			}
			
			@Override
			public WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException {
				return null;
			}
			
			@Override
			public WatchKey take() throws InterruptedException {
				return null;
			}
		};
	}
	
	private static final String regexMetaChars = ".^$+{[]|()";
	private static final String globMetaChars = "\\*?[{";
	private static boolean isRegexMeta(char c) {
		return regexMetaChars.indexOf(c) != -1;
	}
	private static boolean isGlobMeta(char c) {
		return globMetaChars.indexOf(c) != -1;
	}
	private static final char EOL = 0;
	private static char next(String glob, int i) {
		if (i < glob.length()) {
			return glob.charAt(i);
		}
		return EOL;
	}
	
	/*
	 * Creates a regex pattern from the given glob expression.
	 *
	 * @throws  PatternSyntaxException
	 */
	public static String toRegexPattern(String globPattern) {
		boolean inGroup = false;
		StringBuilder regex = new StringBuilder("^");
		
		int i = 0;
		while (i < globPattern.length()) {
			char c = globPattern.charAt(i++);
			switch (c) {
				case '\\':
					// escape special characters
					if (i == globPattern.length()) {
						throw new PatternSyntaxException("No character to escape",
								globPattern, i - 1);
					}
					char next = globPattern.charAt(i++);
					if (isGlobMeta(next) || isRegexMeta(next)) {
						regex.append('\\');
					}
					regex.append(next);
					break;
				case '/':
					regex.append(c);
					break;
				case '[':
					// don't match name separator in class
					regex.append("[[^/]&&[");
					if (next(globPattern, i) == '^') {
						// escape the regex negation char if it appears
						regex.append("\\^");
						i++;
					} else {
						// negation
						if (next(globPattern, i) == '!') {
							regex.append('^');
							i++;
						}
						// hyphen allowed at start
						if (next(globPattern, i) == '-') {
							regex.append('-');
							i++;
						}
					}
					boolean hasRangeStart = false;
					char last = 0;
					while (i < globPattern.length()) {
						c = globPattern.charAt(i++);
						if (c == ']') {
							break;
						}
						if (c == '/') {
							throw new PatternSyntaxException("Explicit 'name separator' in class",
									globPattern, i - 1);
						}
						// TBD: how to specify ']' in a class?
						if (c == '\\' || c == '[' ||
						    c == '&' && next(globPattern, i) == '&') {
							// escape '\', '[' or "&&" for regex class
							regex.append('\\');
						}
						regex.append(c);
						
						if (c == '-') {
							if (!hasRangeStart) {
								throw new PatternSyntaxException("Invalid range",
										globPattern, i - 1);
							}
							if ((c = next(globPattern, i++)) == EOL || c == ']') {
								break;
							}
							if (c < last) {
								throw new PatternSyntaxException("Invalid range",
										globPattern, i - 3);
							}
							regex.append(c);
							hasRangeStart = false;
						} else {
							hasRangeStart = true;
							last = c;
						}
					}
					if (c != ']') {
						throw new PatternSyntaxException("Missing ']", globPattern, i - 1);
					}
					regex.append("]]");
					break;
				case '{':
					if (inGroup) {
						throw new PatternSyntaxException("Cannot nest groups",
								globPattern, i - 1);
					}
					regex.append("(?:(?:");
					inGroup = true;
					break;
				case '}':
					if (inGroup) {
						regex.append("))");
						inGroup = false;
					} else {
						regex.append('}');
					}
					break;
				case ',':
					if (inGroup) {
						regex.append(")|(?:");
					} else {
						regex.append(',');
					}
					break;
				case '*':
					if (next(globPattern, i) == '*') {
						// crosses directory boundaries
						regex.append(".*");
						i++;
					} else {
						// within directory boundary
						regex.append("[^/]*");
					}
					break;
				case '?':
					regex.append("[^/]");
					break;
				default:
					if (isRegexMeta(c)) {
						regex.append('\\');
					}
					regex.append(c);
			}
		}
		if (inGroup) {
			throw new PatternSyntaxException("Missing '}", globPattern, i - 1);
		}
		return regex.append('$').toString();
	}
}
