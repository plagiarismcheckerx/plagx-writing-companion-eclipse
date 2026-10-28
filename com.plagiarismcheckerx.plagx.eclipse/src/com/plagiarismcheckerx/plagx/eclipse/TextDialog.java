package com.plagiarismcheckerx.plagx.eclipse;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** Read-only, selectable text with a Copy button (used for formatted reference lists). */
final class TextDialog extends Dialog {
    private static final int COPY = IDialogConstants.CLIENT_ID + 1;
    private final String title;
    private final String text;

    TextDialog(Shell parent, String title, String text) {
        super(parent);
        this.title = title;
        this.text = text;
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(title);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Text box = new Text(area, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        box.setText(text);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.widthHint = 640;
        data.heightHint = 360;
        box.setLayoutData(data);
        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, COPY, "Copy", false);
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
    }

    @Override
    protected void buttonPressed(int id) {
        if (id == COPY) {
            Clipboard clipboard = new Clipboard(getShell().getDisplay());
            clipboard.setContents(new Object[]{text}, new Transfer[]{TextTransfer.getInstance()});
            clipboard.dispose();
            return;
        }
        super.buttonPressed(id);
    }
}
