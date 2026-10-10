package dev.blocklens.core.builder;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Immutable local state input; validation and comparison are Minecraft independent. */
public record BuilderState(String id, Map<String, String> properties) {
    public static final int MAX_PROPERTIES = 32;
    public static final int UNAVAILABLE = 0, MATCH = 1, ADJUSTED = 2, DIFFERENT = 3;
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    private static final Pattern TOKEN = Pattern.compile("[a-z0-9/._-]+");

    public BuilderState {
        if (id == null || id.length() > 256 || !ID.matcher(id).matches()
                || properties == null || properties.size() > MAX_PROPERTIES) {
            throw new IllegalArgumentException("Invalid local state input");
        }
        var copy = new TreeMap<String, String>();
        for (var entry : properties.entrySet()) {
            if (!token(entry.getKey()) || !token(entry.getValue())) {
                throw new IllegalArgumentException("Invalid state property");
            }
            copy.put(entry.getKey(), entry.getValue());
        }
        properties = Collections.unmodifiableMap(copy);
    }

    public static int compare(BuilderState expected, BuilderState observed) {
        if (expected == null || observed == null) return UNAVAILABLE;
        if (!expected.id.equals(observed.id)) return DIFFERENT;
        return expected.properties.equals(observed.properties) ? MATCH : ADJUSTED;
    }

    private static boolean token(String value) {
        return value != null && value.length() <= 64 && TOKEN.matcher(value).matches();
    }
}
