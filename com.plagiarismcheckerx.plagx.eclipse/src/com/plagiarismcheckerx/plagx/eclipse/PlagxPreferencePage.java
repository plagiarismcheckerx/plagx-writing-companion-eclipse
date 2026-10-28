package com.plagiarismcheckerx.plagx.eclipse;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/** Window | Preferences | Plagx Writing Companion. */
public final class PlagxPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    public PlagxPreferencePage() {
        super(GRID);
        setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, PlagxPrefs.BUNDLE));
        setDescription("Academic writing checks for Markdown, text, LaTeX and BibTeX files. By Plagiarism Checker X. "
                + "This free plugin gives writing suggestions only; it does not detect plagiarism or AI-generated text.");
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected void createFieldEditors() {
        addField(new ComboFieldEditor(PlagxPrefs.STYLE, "Citation style:",
                new String[][]{{"APA", "apa"}, {"MLA", "mla"}}, getFieldEditorParent()));
        addField(new IntegerFieldEditor(PlagxPrefs.MIN_WORDS, "Minimum words (0 = no limit):", getFieldEditorParent()));
        addField(new IntegerFieldEditor(PlagxPrefs.MAX_WORDS, "Maximum words (0 = no limit):", getFieldEditorParent()));
        addField(new StringFieldEditor(PlagxPrefs.HEADINGS, "Required headings (comma separated):",
                getFieldEditorParent()));
        addField(new StringFieldEditor(PlagxPrefs.EXTENSIONS, "File extensions checked:", getFieldEditorParent()));
        addField(new BooleanFieldEditor(PlagxPrefs.CHECK_ON_SAVE, "Check files when saved", getFieldEditorParent()));
        for (String c : PlagxPrefs.CHECKS) {
            addField(new BooleanFieldEditor(PlagxPrefs.CHECK_PREFIX + c, "Check: " + c, getFieldEditorParent()));
        }
    }
}
