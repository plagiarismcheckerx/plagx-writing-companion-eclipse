package com.plagiarismcheckerx.plagx.eclipse;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IStartup;

/** Checks drafts and BibTeX files whenever they are saved. Registered at workbench startup. */
public final class SaveListener implements IResourceChangeListener, IStartup {
    private static SaveListener installed;

    @Override
    public void earlyStartup() {
        install();
    }

    public static synchronized void install() {
        if (installed == null) {
            installed = new SaveListener();
            ResourcesPlugin.getWorkspace().addResourceChangeListener(installed, IResourceChangeEvent.POST_CHANGE);
        }
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        if (!PlagxPrefs.checkOnSave() || event.getDelta() == null) return;
        List<IFile> files = new ArrayList<>();
        try {
            event.getDelta().accept(delta -> {
                if (delta.getResource().getType() != IResource.FILE) return true;
                IFile file = (IFile) delta.getResource();
                boolean changed = delta.getKind() == IResourceDelta.ADDED
                        || (delta.getKind() == IResourceDelta.CHANGED && (delta.getFlags() & IResourceDelta.CONTENT) != 0);
                if (changed && (PlagxChecker.isDraft(file) || PlagxChecker.isBib(file))) files.add(file);
                return false;
            });
        } catch (CoreException e) {
            return;
        }
        if (!files.isEmpty()) schedule(files);
    }

    static void schedule(List<IFile> files) {
        Job job = new Job("Plagx: checking drafts") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                for (IFile file : files) {
                    if (!file.exists()) continue;
                    try {
                        PlagxChecker.check(file, PlagxChecker.read(file));
                    } catch (Exception e) {
                        return Status.error("Plagx check failed for " + file.getFullPath(), e);
                    }
                }
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        job.schedule();
    }
}
