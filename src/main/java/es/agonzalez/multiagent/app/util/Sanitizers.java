package es.agonzalez.multiagent.app.util;

/** Utility sanitization helpers to centralize repeated normalization logic. */
public final class Sanitizers {
    private Sanitizers() {}

    /** Remove CR/LF characters to prevent log/file injection. */
    public static String stripNewlines(String in) {
        if (in == null) return null;
        return in.replaceAll("[\r\n]", "");
    }

    /** Trim trailing slashes (one or more) except when the whole string is just "/". */
    public static String trimTrailingSlashes(String in) {
        if (in == null) return null;
        if (in.equals("/")) return in;
        return in.replaceAll("/+$(?!/)", "");
    }

    /** Composite normalization: strip newlines then trim trailing slashes. */
    public static String normalizePathLike(String in) {
        return trimTrailingSlashes(stripNewlines(in));
    }
}
