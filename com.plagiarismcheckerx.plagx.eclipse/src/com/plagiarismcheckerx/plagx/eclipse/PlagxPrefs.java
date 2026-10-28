package com.plagiarismcheckerx.plagx.eclipse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

/** Preference keys, defaults and typed access. Stored in the instance scope of this bundle. */
public final class PlagxPrefs {
    public static final String BUNDLE = "com.plagiarismcheckerx.plagx.eclipse";
    public static final String STYLE = "style";
    public static final String MIN_WORDS = "minWords";
    public static final String MAX_WORDS = "maxWords";
    public static final String HEADINGS = "requiredHeadings";
    public static final String EXTENSIONS = "extensions";
    public static final String CHECK_ON_SAVE = "checkOnSave";
    public static final String CHECK_PREFIX = "check.";
    public static final List<String> CHECKS = List.of("length", "references", "punctuation", "grammar", "passive-voice");

    private PlagxPrefs() {
    }

    private static String get(String key) {
        return Platform.getPreferencesService().getString(BUNDLE, key, "", null);
    }

    public static String style() {
        String s = get(STYLE);
        return s.isEmpty() ? "apa" : s;
    }

    public static int minWords() {
        return Platform.getPreferencesService().getInt(BUNDLE, MIN_WORDS, 0, null);
    }

    public static int maxWords() {
        return Platform.getPreferencesService().getInt(BUNDLE, MAX_WORDS, 0, null);
    }

    public static boolean checkOnSave() {
        return Platform.getPreferencesService().getBoolean(BUNDLE, CHECK_ON_SAVE, true, null);
    }

    public static List<String> headings() {
        return split(get(HEADINGS));
    }

    public static List<String> extensions() {
        List<String> out = new ArrayList<>();
        for (String e : split(get(EXTENSIONS))) out.add(e.replaceFirst("^\\.", "").toLowerCase(Locale.ROOT));
        return out;
    }

    public static List<String> checks() {
        List<String> out = new ArrayList<>();
        for (String c : CHECKS) {
            if (Platform.getPreferencesService().getBoolean(BUNDLE, CHECK_PREFIX + c, false, null)) out.add(c);
        }
        return out;
    }

    static List<String> split(String value) {
        List<String> out = new ArrayList<>();
        Arrays.stream(value.split(",")).map(String::trim).filter(v -> !v.isEmpty()).forEach(out::add);
        return out;
    }

    /** Registered through org.eclipse.core.runtime.preferences. */
    public static final class Initializer extends AbstractPreferenceInitializer {
        @Override
        public void initializeDefaultPreferences() {
            IEclipsePreferences d = DefaultScope.INSTANCE.getNode(BUNDLE);
            d.put(STYLE, "apa");
            d.putInt(MIN_WORDS, 0);
            d.putInt(MAX_WORDS, 0);
            d.put(HEADINGS, "");
            d.put(EXTENSIONS, "md, markdown, txt, tex");
            d.putBoolean(CHECK_ON_SAVE, true);
            for (String c : CHECKS) d.putBoolean(CHECK_PREFIX + c, !"passive-voice".equals(c));
        }
    }
}
