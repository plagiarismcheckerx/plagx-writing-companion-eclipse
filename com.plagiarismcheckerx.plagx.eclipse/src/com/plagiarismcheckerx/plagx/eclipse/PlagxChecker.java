package com.plagiarismcheckerx.plagx.eclipse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.plagiarismcheckerx.plagx.cite.CheckIssue;
import com.plagiarismcheckerx.plagx.cite.CheckResult;
import com.plagiarismcheckerx.plagx.cite.PlagxCite;
import com.plagiarismcheckerx.plagx.preflight.DraftIssue;
import com.plagiarismcheckerx.plagx.preflight.DraftResult;
import com.plagiarismcheckerx.plagx.preflight.PlagxPreflight;

/** Workbench-independent glue between Eclipse resources and the Plagx Cite / Preflight cores. */
public final class PlagxChecker {
    public static final String MARKER = "com.plagiarismcheckerx.plagx.eclipse.problem";
    public static final String ATTR_SUGGESTION = "suggestion";
    public static final String ATTR_CODE = "code";

    private PlagxChecker() {
    }

    public static boolean isBib(IFile file) {
        return file != null && "bib".equalsIgnoreCase(file.getFileExtension());
    }

    public static boolean isDraft(IFile file) {
        String ext = file == null ? null : file.getFileExtension();
        return ext != null && !isBib(file) && PlagxPrefs.extensions().contains(ext.toLowerCase(Locale.ROOT));
    }

    public static String read(IFile file) throws CoreException, IOException {
        try (InputStream in = file.getContents(true)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            in.transferTo(out);
            return out.toString(Charset.forName(file.getCharset()));
        }
    }

    public static PlagxPreflight.Options options() {
        PlagxPreflight.Options o = PlagxPreflight.options().style(PlagxPrefs.style());
        if (PlagxPrefs.minWords() > 0) o.minWords(PlagxPrefs.minWords());
        if (PlagxPrefs.maxWords() > 0) o.maxWords(PlagxPrefs.maxWords());
        if (!PlagxPrefs.checks().isEmpty()) o.checks(PlagxPrefs.checks());
        if (!PlagxPrefs.headings().isEmpty()) o.requiredHeadings(PlagxPrefs.headings());
        return o;
    }

    /** Checks a draft (or BibTeX file, offline) and replaces its Plagx markers. Returns the draft result or null. */
    public static DraftResult check(IFile file, String text) throws CoreException {
        if (isBib(file)) {
            markBib(file, text, PlagxCite.checkReferences(PlagxCite.parseBibTeX(text)));
            return null;
        }
        if (!isDraft(file)) return null;
        DraftResult result = PlagxPreflight.checkDraft(text, options());
        file.deleteMarkers(MARKER, false, IResource.DEPTH_ZERO);
        for (DraftIssue issue : result.getIssues()) {
            int start = Math.max(0, Math.min(issue.getStart(), text.length()));
            int end = Math.max(start + (start < text.length() ? 1 : 0), Math.min(issue.getEnd(), text.length()));
            IMarker m = file.createMarker(MARKER);
            m.setAttributes(new String[]{IMarker.MESSAGE, IMarker.SEVERITY, IMarker.CHAR_START, IMarker.CHAR_END,
                    IMarker.LINE_NUMBER, ATTR_CODE, ATTR_SUGGESTION},
                    new Object[]{issue.getMessage(), severity(issue.getLevel()), start, end, issue.getLine(),
                            issue.getCode(), issue.getSuggestion()});
        }
        return result;
    }

    public static void markBib(IFile file, String text, CheckResult result) throws CoreException {
        file.deleteMarkers(MARKER, false, IResource.DEPTH_ZERO);
        for (CheckIssue issue : result.getIssues()) {
            int start = bibKeyOffset(text, issue.getRef());
            int lineEnd = text.indexOf('\n', start);
            int end = lineEnd < 0 ? text.length() : lineEnd;
            IMarker m = file.createMarker(MARKER);
            m.setAttributes(new String[]{IMarker.MESSAGE, IMarker.SEVERITY, IMarker.CHAR_START, IMarker.CHAR_END,
                    IMarker.LINE_NUMBER, ATTR_CODE},
                    new Object[]{issue.getRef() + ": " + issue.getMessage(), severity(issue.getLevel()), start,
                            Math.max(start, end), lineOf(text, start), issue.getCode()});
        }
    }

    static int severity(String level) {
        switch (level) {
            case "error":
                return IMarker.SEVERITY_ERROR;
            case "warning":
                return IMarker.SEVERITY_WARNING;
            default:
                return IMarker.SEVERITY_INFO;
        }
    }

    static int lineOf(String text, int offset) {
        int line = 1;
        for (int i = 0; i < offset && i < text.length(); i++) if (text.charAt(i) == '\n') line++;
        return line;
    }

    public static int bibKeyOffset(String bibtex, String key) {
        if (key == null || key.startsWith("#")) return 0;
        Matcher m = Pattern.compile("(?m)^\\s*@\\w+\\s*\\{\\s*" + Pattern.quote(key) + "\\s*,").matcher(bibtex);
        return m.find() ? m.start() : 0;
    }

    public static List<String> bibliography(String bibtex) {
        List<Map<String, Object>> refs = PlagxCite.parseBibTeX(bibtex);
        return PlagxCite.formatBibliography(refs, PlagxPrefs.style(), "text");
    }

    /** "Ready" when there are no errors; warnings are listed but don't block. */
    public static String readiness(DraftResult r) {
        String label = r.getErrors() > 0 ? "Not ready" : r.getWarnings() > 0 ? "Ready with suggestions" : "Ready";
        return String.format(Locale.ROOT, "%s%n%n%,d words, %d min read, Flesch %d%n%d errors, %d warnings, %d notes",
                label, r.getStats().getWords(), Math.max(1, r.getStats().getReadingTimeMinutes()),
                Math.round(r.getStats().getFleschReadingEase()), r.getErrors(), r.getWarnings(), r.getInfos());
    }
}
