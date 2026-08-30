package com.cryptostrategy.platform.strategy.api.model;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record SemanticVersion(int major, int minor, int patch) implements Comparable<SemanticVersion> {
    private static final Pattern FORMAT = Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$");
    public SemanticVersion {
        if (major < 0 || minor < 0 || patch < 0) throw new IllegalArgumentException("Version parts must be nonnegative");
    }
    public static SemanticVersion parse(String value) {
        Objects.requireNonNull(value, "value"); Matcher matcher = FORMAT.matcher(value);
        if (!matcher.matches()) throw new IllegalArgumentException("Version must use MAJOR.MINOR.PATCH");
        return new SemanticVersion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
    }
    @Override public int compareTo(SemanticVersion other) {
        int result = Integer.compare(major, other.major);
        if (result == 0) result = Integer.compare(minor, other.minor);
        return result == 0 ? Integer.compare(patch, other.patch) : result;
    }
    @Override public String toString() { return major + "." + minor + "." + patch; }
}
