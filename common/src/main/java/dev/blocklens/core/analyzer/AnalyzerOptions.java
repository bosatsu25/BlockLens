package dev.blocklens.core.analyzer;

import java.util.Arrays;

/** Immutable per-analyzer radius, vertical radius, marker and minimum-interval options. */
public final class AnalyzerOptions {
    private static final int[][] DEFAULTS = {{64,64,64,20},{5,3,12,10},{16,8,8,20},{16,8,8,20},{16,8,12,20}};
    private static final int[][] MIN = {{16,16,8,5},{1,1,1,5},{1,1,1,5},{1,1,1,5},{1,1,1,5}};
    private static final int[][] MAX = {{256,256,128,100},{8,5,24,100},{32,16,16,100},{32,16,16,100},{32,16,24,100}};
    private final int[][] values;

    private AnalyzerOptions(int[][] values) { this.values = values; }
    public static AnalyzerOptions defaults() { return new AnalyzerOptions(copy(DEFAULTS)); }
    public int value(int kind, int field) { return values[kind][field]; }
    public static int minimum(int kind, int field) { return MIN[kind][field]; }
    public static int maximum(int kind, int field) { return MAX[kind][field]; }

    public AnalyzerOptions with(int kind, int field, int value) {
        if (value < minimum(kind, field) || value > maximum(kind, field)) {
            throw new IllegalArgumentException("analyzer option outside bounds");
        }
        int[][] changed = copy(values);
        changed[kind][field] = value;
        if (kind == 0 && field <= 1) { changed[0][0] = value; changed[0][1] = value; }
        return new AnalyzerOptions(changed);
    }
    private static int[][] copy(int[][] source) {
        return Arrays.stream(source).map(int[]::clone).toArray(int[][]::new);
    }
    @Override public boolean equals(Object other) {
        return other instanceof AnalyzerOptions options && Arrays.deepEquals(values, options.values);
    }
    @Override public int hashCode() { return Arrays.deepHashCode(values); }
}
