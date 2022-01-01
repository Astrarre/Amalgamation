package io.github.astrarre.amalgamation.gradle.mixin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class MemberMatcher {
    /**
     * Regex selector, searches for targets using supplied regular expression
     */
    private static final Pattern PATTERN = Pattern.compile("((owner|name|desc)\\s*=\\s*)?/(.*?)(?<!\\\\)/");
    
    /**
     * Names of the positional source elements, just used for error messages
     */
    private static final String[] PATTERN_SOURCE_NAMES = { "owner", "name", "desc" };
    
    // Positional source elements
    private static final int SOURCE_OWNER = 0;
    private static final int SOURCE_NAME = 1;
    private static final int SOURCE_DESC = 2;
    
    /**
     * Positional patterns. The match sources are packed into 3-element arrays
     * just to make the iteration for matching simpler
     */
    private final Pattern[] patterns;
    
    /**
     * Stored exception during parse. The contract of parse prohibits us from
     * emitting (intentional) exceptions. Any exceptions are stored here so they
     * can be emitted in {@link #validate}. 
     */
    private final Exception parseException;
    
    /**
     * Input string, stored just so we can emit it along with the exception
     * message if {@link #validate} needs to throw a parse exception
     */
    private final String input;
    
    private MemberMatcher(Pattern[] patterns, Exception parseException, String input) {
        this.patterns = patterns;
        this.parseException = parseException;
        this.input = input;
    }
    
    /**
     * Parse a MemberMatcher from the supplied input string.
     * 
     * @param input Raw input string
     * @return parsed MemberMatcher
     */
    public static MemberMatcher parse(final String input) {
        Matcher matcher = MemberMatcher.PATTERN.matcher(input);
        Pattern[] patterns = new Pattern[3];
        Exception parseException = null;
        
        while (matcher.find()) {
            Pattern pattern;
            try {
                pattern = Pattern.compile(matcher.group(3));
            } catch (PatternSyntaxException ex) {
                parseException = ex;
                pattern = Pattern.compile(".*");
                ex.printStackTrace();
            }
            
            int patternId = "owner".equals(matcher.group(2)) ? MemberMatcher.SOURCE_OWNER : "desc".equals(matcher.group(2))
                    ? MemberMatcher.SOURCE_DESC : MemberMatcher.SOURCE_NAME;
            if (patterns[patternId] != null) {
                parseException = new IllegalArgumentException("Pattern for '" + MemberMatcher.PATTERN_SOURCE_NAMES[patternId]
                        + "' specified multiple times: Old=/" + patterns[patternId].pattern() + "/ New=/" + pattern.pattern() + "/");
            }
            patterns[patternId] = pattern;
        }

        return new MemberMatcher(patterns, parseException, input);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #validate()
     */
    public MemberMatcher validate() throws IllegalArgumentException {
        if (this.parseException != null) {
            if (this.parseException instanceof IllegalArgumentException) {
                throw (IllegalArgumentException)this.parseException;
            }
            throw new IllegalArgumentException("Error parsing regex selector", this.parseException);
        }
        
        boolean validPattern = false;
        for (Pattern pattern : this.patterns) {
            validPattern |= pattern != null;
        }
        
        if (!validPattern) {
            throw new IllegalArgumentException("Error parsing regex selector, the input was in an unexpected format: " + this.input);
        }
        
        return this;
    }
    
    @Override
    public String toString() {
        return this.input;
    }
    
    public boolean match(String owner, String name, String desc) {
        return this.matches(owner, name, desc);
    }

    private boolean matches(String... args) {
        boolean result = false;
//        boolean exactOnly = true;
        
        for (int i = 0; i < this.patterns.length; i++) {
            if (this.patterns[i] == null || args[i] == null) {
                continue;
            }
            
            if (this.patterns[i].matcher(args[i]).find()) {
//                String pattern = this.patterns[i].pattern();
//                if (pattern.startsWith("^") && pattern.endsWith("$") && exactOnly) {
                    result = true;
//                } else {
//                    result = MatchResult.EXACT_MATCH;
//                    exactOnly = false;
//                }
            } else {
                result = false;
            }
        }
        
        return result;
    }

}