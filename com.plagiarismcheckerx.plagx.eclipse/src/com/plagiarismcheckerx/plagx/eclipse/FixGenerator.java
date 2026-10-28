package com.plagiarismcheckerx.plagx.eclipse;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

/** Quick Fix (Ctrl+1) for Plagx markers that carry a suggestion. */
public final class FixGenerator implements IMarkerResolutionGenerator2 {
    @Override
    public boolean hasResolutions(IMarker marker) {
        return marker.getAttribute(PlagxChecker.ATTR_SUGGESTION, null) != null;
    }

    @Override
    public IMarkerResolution[] getResolutions(IMarker marker) {
        return hasResolutions(marker) ? new IMarkerResolution[]{new Replace()} : new IMarkerResolution[0];
    }

    /** Replaces the flagged text with the suggestion and saves the file. */
    public static void apply(IMarker marker) throws Exception {
        String suggestion = marker.getAttribute(PlagxChecker.ATTR_SUGGESTION, null);
        int start = marker.getAttribute(IMarker.CHAR_START, -1);
        int end = marker.getAttribute(IMarker.CHAR_END, -1);
        if (suggestion == null || start < 0 || end < start) return;
        IFile file = (IFile) marker.getResource();
        IPath path = file.getFullPath();
        ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
        manager.connect(path, LocationKind.IFILE, null);
        try {
            ITextFileBuffer buffer = manager.getTextFileBuffer(path, LocationKind.IFILE);
            IDocument doc = buffer.getDocument();
            if (end <= doc.getLength()) {
                doc.replace(start, end - start, suggestion);
                marker.delete();
                buffer.commit(null, true);
            }
        } finally {
            manager.disconnect(path, LocationKind.IFILE, null);
        }
    }

    private static final class Replace implements IMarkerResolution {
        @Override
        public String getLabel() {
            return "Plagx: apply suggestion";
        }

        @Override
        public void run(IMarker marker) {
            try {
                apply(marker);
            } catch (Exception e) {
                Handlers.error("Plagx: could not apply the fix", e);
            }
        }
    }
}
