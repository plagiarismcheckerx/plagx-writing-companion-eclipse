package com.plagiarismcheckerx.plagx.eclipse.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.plagiarismcheckerx.plagx.eclipse.FixGenerator;
import com.plagiarismcheckerx.plagx.eclipse.PlagxChecker;
import com.plagiarismcheckerx.plagx.eclipse.PlagxPrefs;
import com.plagiarismcheckerx.plagx.preflight.DraftResult;

/** Runs in a headless Eclipse workspace: markers, quick fix and BibTeX checks on real files. */
public class PlagxCheckerTest {
    private IProject project;

    private static String sample(String name) throws IOException {
        try (InputStream in = PlagxCheckerTest.class.getResourceAsStream(name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private IFile create(String name, String text) throws CoreException {
        IFile file = project.getFile(name);
        file.create(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), true, null);
        file.setCharset("UTF-8", null);
        return file;
    }

    private static List<IMarker> markers(IFile file) throws CoreException {
        return Arrays.asList(file.findMarkers(PlagxChecker.MARKER, false, IResource.DEPTH_ZERO));
    }

    @Before
    public void setUp() throws CoreException {
        project = ResourcesPlugin.getWorkspace().getRoot().getProject("plagx-test");
        if (!project.exists()) project.create(null);
        project.open(null);
    }

    @After
    public void tearDown() throws CoreException {
        project.delete(true, true, null);
    }

    @Test
    public void defaultsComeFromThePreferenceInitializer() {
        assertEquals("apa", PlagxPrefs.style());
        assertTrue(PlagxPrefs.extensions().contains("md"));
        assertTrue(PlagxPrefs.checks().contains("grammar"));
        assertFalse(PlagxPrefs.checks().contains("passive-voice"));
    }

    @Test
    public void draftGetsProblemMarkersWithSuggestions() throws Exception {
        IFile file = create("essay.md", sample("sample-essay.md"));
        DraftResult result = PlagxChecker.check(file, PlagxChecker.read(file));
        assertNotNull(result);
        List<IMarker> markers = markers(file);
        assertEquals(result.getIssues().size(), markers.size());
        assertTrue(markers.stream().anyMatch(m -> "repeated-word".equals(m.getAttribute(PlagxChecker.ATTR_CODE, ""))));
        assertTrue(markers.stream().anyMatch(m -> m.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR));
        assertTrue(markers.stream().allMatch(m -> m.getAttribute(IMarker.CHAR_END, -1) > m.getAttribute(IMarker.CHAR_START, -1)));
    }

    @Test
    public void quickFixRewritesTheFile() throws Exception {
        IFile file = create("essay.txt", sample("sample-essay.md"));
        PlagxChecker.check(file, PlagxChecker.read(file));
        IMarker repeated = markers(file).stream()
                .filter(m -> "repeated-word".equals(m.getAttribute(PlagxChecker.ATTR_CODE, "")))
                .findFirst().orElseThrow();
        assertTrue(new FixGenerator().hasResolutions(repeated));
        FixGenerator.apply(repeated);
        assertFalse(PlagxChecker.read(file).contains("to to"));
    }

    @Test
    public void otherFilesAreIgnored() throws Exception {
        IFile file = create("Main.java", sample("sample-essay.md"));
        assertNull(PlagxChecker.check(file, PlagxChecker.read(file)));
        assertTrue(markers(file).isEmpty());
    }

    @Test
    public void bibtexProblemsPointAtTheirEntries() throws Exception {
        String bib = sample("sample.bib");
        IFile file = create("refs.bib", bib);
        PlagxChecker.check(file, bib);
        List<IMarker> markers = markers(file);
        IMarker missing = markers.stream().filter(m -> "missing-title".equals(m.getAttribute(PlagxChecker.ATTR_CODE, "")))
                .findFirst().orElseThrow();
        assertEquals(PlagxChecker.bibKeyOffset(bib, "notitle"), missing.getAttribute(IMarker.CHAR_START, -1));
        assertTrue(markers.stream().anyMatch(m -> "duplicate".equals(m.getAttribute(PlagxChecker.ATTR_CODE, ""))));
        assertTrue(PlagxChecker.bibliography(bib).stream().anyMatch(e -> e.contains("Smith, J. (2020)")));
    }
}
