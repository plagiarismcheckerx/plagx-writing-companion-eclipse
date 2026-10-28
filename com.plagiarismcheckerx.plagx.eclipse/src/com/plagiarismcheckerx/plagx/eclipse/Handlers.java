package com.plagiarismcheckerx.plagx.eclipse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import com.plagiarismcheckerx.plagx.cite.CheckResult;
import com.plagiarismcheckerx.plagx.cite.PlagxCite;
import com.plagiarismcheckerx.plagx.preflight.DraftResult;
import com.plagiarismcheckerx.plagx.preflight.PlagxPreflight;

/** Command handlers for the Plagx menu. */
public final class Handlers {
    private Handlers() {
    }

    static void error(String message, Throwable e) {
        Display.getDefault().asyncExec(() -> ErrorDialog.openError(shell(), "Plagx", message,
                Status.error(e.getMessage() == null ? e.toString() : e.getMessage(), e)));
    }

    static Shell shell() {
        return PlatformUI.getWorkbench().getDisplay().getActiveShell();
    }

    /** The active text editor, its file and document; null fields when not available. */
    record Target(ITextEditor editor, IFile file, IDocument document) {
        static Target of(ExecutionEvent event) {
            IEditorPart part = HandlerUtil.getActiveEditor(event);
            if (!(part instanceof ITextEditor editor)) return new Target(null, null, null);
            IFile file = part.getEditorInput() instanceof IFileEditorInput in ? in.getFile() : null;
            IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            return new Target(editor, file, doc);
        }
    }

    public static final class CheckDraft extends AbstractHandler {
        @Override
        public Object execute(ExecutionEvent event) {
            Target t = Target.of(event);
            if (t.file() == null || t.document() == null) return null;
            try {
                PlagxChecker.check(t.file(), t.document().get());
            } catch (Exception e) {
                error("Plagx: check failed", e);
            }
            return null;
        }
    }

    public static final class Readiness extends AbstractHandler {
        @Override
        public Object execute(ExecutionEvent event) {
            Target t = Target.of(event);
            if (t.document() == null) return null;
            DraftResult r = PlagxPreflight.checkDraft(t.document().get(), PlagxChecker.options());
            MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Plagx writing readiness",
                    PlagxChecker.readiness(r));
            return null;
        }
    }

    public static final class FormatBibliography extends AbstractHandler {
        @Override
        public Object execute(ExecutionEvent event) {
            Target t = Target.of(event);
            if (t.document() == null) return null;
            List<String> entries = PlagxChecker.bibliography(t.document().get());
            String heading = "mla".equals(PlagxPrefs.style()) ? "Works Cited" : "References";
            if (entries.isEmpty()) {
                MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Plagx", "No BibTeX entries found.");
                return null;
            }
            new TextDialog(HandlerUtil.getActiveShell(event), "Plagx: " + heading,
                    heading + "\n\n" + String.join("\n\n", entries)).open();
            return null;
        }
    }

    public static final class CheckBibOnline extends AbstractHandler {
        @Override
        public Object execute(ExecutionEvent event) {
            Target t = Target.of(event);
            if (t.file() == null || t.document() == null || !PlagxChecker.isBib(t.file())) return null;
            String text = t.document().get();
            IFile file = t.file();
            Job job = new Job("Plagx: checking references and DOIs") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        CheckResult r = PlagxCite.checkReferences(PlagxCite.parseBibTeX(text), true, null, null);
                        PlagxChecker.markBib(file, text, r);
                        Display.getDefault().asyncExec(() -> MessageDialog.openInformation(shell(), "Plagx",
                                r.getReferences() + " references, " + r.getErrors() + " errors, " + r.getWarnings()
                                        + " warnings. See the Problems view."));
                    } catch (Exception e) {
                        error("Plagx: reference check failed", e);
                    }
                    return Status.OK_STATUS;
                }
            };
            job.setUser(true);
            job.schedule();
            return null;
        }
    }

    public static final class CiteDoi extends AbstractHandler {
        @Override
        public Object execute(ExecutionEvent event) {
            Target t = Target.of(event);
            if (t.editor() == null || t.document() == null) return null;
            InputDialog input = new InputDialog(HandlerUtil.getActiveShell(event), "Plagx: Insert Reference from DOI",
                    "DOI or doi.org link (e.g. 10.1037/0003-066X.59.1.29):", "",
                    v -> PlagxCite.normalizeDoi(v) == null ? "Enter a valid DOI" : null);
            if (input.open() != Window.OK) return null;
            String doi = PlagxCite.normalizeDoi(input.getValue());
            Job job = new Job("Plagx: looking up DOI on Crossref") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        Map<String, Object> ref = PlagxCite.lookupDoi(doi);
                        String style = PlagxPrefs.style();
                        Map<String, String> choices = new LinkedHashMap<>();
                        choices.put(PlagxCite.formatReference(ref, style), "Reference list entry");
                        choices.put(PlagxCite.citeInText(ref, style), "In-text citation");
                        choices.put(PlagxCite.citeInText(ref, style, null, true), "Narrative citation");
                        Display.getDefault().asyncExec(() -> insert(t, choices));
                    } catch (Exception e) {
                        error("Plagx: DOI lookup failed", e);
                    }
                    return Status.OK_STATUS;
                }
            };
            job.setUser(true);
            job.schedule();
            return null;
        }

        private static void insert(Target t, Map<String, String> choices) {
            ElementListSelectionDialog dialog = new ElementListSelectionDialog(shell(),
                    new org.eclipse.jface.viewers.LabelProvider() {
                        @Override
                        public String getText(Object element) {
                            return choices.get(element) + ": " + element;
                        }
                    });
            dialog.setTitle("Plagx: Insert");
            dialog.setMessage("Choose what to insert:");
            dialog.setElements(choices.keySet().toArray());
            if (dialog.open() != Window.OK || dialog.getFirstResult() == null) return;
            String text = (String) dialog.getFirstResult();
            ITextSelection sel = (ITextSelection) t.editor().getSelectionProvider().getSelection();
            try {
                t.document().replace(sel.getOffset(), sel.getLength(), text);
            } catch (Exception e) {
                error("Plagx: could not insert the reference", e);
            }
        }
    }
}
