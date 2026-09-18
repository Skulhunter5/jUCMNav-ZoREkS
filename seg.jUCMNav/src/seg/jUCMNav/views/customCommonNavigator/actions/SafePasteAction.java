/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Andrey Loskutov <loskutov@gmx.de> - generified interface, bug 462760
 *******************************************************************************/
package seg.jUCMNav.views.customCommonNavigator.actions;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CopyFilesAndFoldersOperation;
import org.eclipse.ui.actions.CopyProjectOperation;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.eclipse.ui.part.ResourceTransfer;

/**
 * The platform's internal navigator PasteAction with a safety gate around
 * {@link ResourceTransfer}.
 * <p>
 * The platform variant unconditionally reads the clipboard through
 * {@link ResourceTransfer} on every selection change. {@link ResourceTransfer}
 * treats the first 4 bytes of whatever the clipboard owner serves for its
 * target as a resource count; a clipboard owned by an unrelated application
 * can therefore crash with a {@link NegativeArraySizeException} or make the
 * platform log "Transfer aborted, too many resources" from inside
 * {@code ResourceTransfer.nativeToJava} (see Eclipse bug 205678). This clone
 * only reads the {@link IResource} payload when the clipboard was written by
 * this plug-in's own {@link SafeCopyAction} (detected via
 * {@link JUCMResourceCopyMarkerTransfer}), and swallows the malformed-count
 * failure modes that escape the platform's own guard. File transfers from
 * external file managers are handled exactly as before.
 *
 * @author skuly
 */
/*package*/class SafePasteAction extends SelectionListenerAction {

	/**
	 * The id of this action.
	 */
	public static final String ID = "seg.jUCMNav.views.customCommonNavigator.actions.SafePasteAction"; //$NON-NLS-1$

	/**
	 * The shell in which to show any dialogs.
	 */
	private final Shell shell;

	/**
	 * System clipboard
	 */
	private final Clipboard clipboard;

	/**
	 * Creates a new action.
	 *
	 * @param shell the shell for any dialogs
	 * @param clipboard the clipboard
	 */
	public SafePasteAction(Shell shell, Clipboard clipboard) {
		super("Paste");
		Assert.isNotNull(shell);
		Assert.isNotNull(clipboard);
		this.shell = shell;
		this.clipboard = clipboard;
		setToolTipText("Paste selected resource(s)");
		setId(SafePasteAction.ID);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, "HelpId"); //$NON-NLS-1$
	}

	/**
	 * Returns the resources held by the clipboard, but only when the clipboard
	 * was written by this plug-in's {@link SafeCopyAction}. For any other
	 * clipboard owner the resource payload is never requested, so a foreign
	 * clipboard cannot make {@link ResourceTransfer} throw or log from inside
	 * the platform.
	 *
	 * @return the copied resources, or <code>null</code> when the clipboard is
	 *         not a trusted jUCMNav resource copy
	 */
	private IResource[] getClipboardResourceData() {
		final IResource[][] clipboardData = new IResource[1][];
		shell.getDisplay().syncExec(() -> {
			// Only read ResourceTransfer when our marker is on the clipboard.
			if (clipboard.getContents(JUCMResourceCopyMarkerTransfer.INSTANCE) == null) {
				clipboardData[0] = null;
				return;
			}
			ResourceTransfer resTransfer = ResourceTransfer.getInstance();
			try {
				clipboardData[0] = (IResource[]) clipboard.getContents(resTransfer);
			} catch (NegativeArraySizeException e) {
				// count decoded from the payload is negative; treat as "no resources"
				clipboardData[0] = null;
			} catch (IllegalArgumentException e) {
				// "Unknown resource type" (bug 205678 companion); treat as "no resources"
				clipboardData[0] = null;
			}
		});
		return clipboardData[0];
	}

	/**
	 * Returns the actual target of the paste action. Returns null
	 * if no valid target is selected.
	 *
	 * @param clipboardContent current content of the clipboard
	 *
	 * @return the actual target of the paste action
	 */
	private IResource getTarget(IResource[] clipboardContent) {
		List<? extends IResource> selectedResources = getSelectedResources();

		// selection is copied to itself => copy to parent
		if (clipboardContent != null && areEqualsUnordered(selectedResources, Arrays.asList(clipboardContent))) {
			return selectedResources.get(0).getParent();
		}

		for (IResource resource : selectedResources) {
			if (resource instanceof IProject && !((IProject) resource).isOpen()) {
				return null;
			}
			if (resource.getType() == IResource.FILE) {
				resource = resource.getParent();
			}
			if (resource != null) {
				return resource;
			}
		}
		return null;
	}

	/**
	 * @param a The first collection
	 * @param b The second collection
	 * @return true if both collections aren't null and they contain the exact same
	 *         items, regardless of their order.
	 */
	private boolean areEqualsUnordered(Collection<? extends IResource> a, Collection<? extends IResource> b) {
		return b != null && a != null && !a.isEmpty() // they are not empty...
				&& a.size() == b.size() // ... and they have the same size
				&& a.containsAll(b); // ... and all elements of A are in B
	}

	/**
	 * Returns whether any of the given resources are linked resources.
	 *
	 * @param resources resource to check for linked type. may be null
	 * @return true=one or more resources are linked. false=none of the
	 * 	resources are linked
	 */
	private boolean isLinked(IResource[] resources) {
		for (IResource resource : resources) {
			if (resource.isLinked()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Implementation of method defined on <code>IAction</code>.
	 */
	@Override
	public void run() {
		// try a trusted jUCMNav resource transfer
		IResource[] resourceData = getClipboardResourceData();

		if (resourceData != null && resourceData.length > 0) {
			if (resourceData[0].getType() == IResource.PROJECT) {
				// enablement checks for all projects
				for (IResource resource : resourceData) {
					CopyProjectOperation operation = new CopyProjectOperation(shell);
					operation.copyProject((IProject) resource);
				}
			} else {
				// enablement should ensure that we always have access to a container
				IContainer container = getContainer(resourceData);
				CopyFilesAndFoldersOperation operation = new CopyFilesAndFoldersOperation(shell);
				operation.copyResources(resourceData, container);
			}
			return;
		}

		// try a file transfer
		FileTransfer fileTransfer = FileTransfer.getInstance();
		String[] fileData = (String[]) clipboard.getContents(fileTransfer);

		if (fileData != null) {
			// enablement should ensure that we always have access to a container
			IContainer container = getContainer(null);
			CopyFilesAndFoldersOperation operation = new CopyFilesAndFoldersOperation(shell);
			operation.copyFiles(fileData, container);
		}
	}

	/**
	 * Returns the container to hold the pasted resources.
	 *
	 * @param clipboardContent current content of the clipboard
	 */
	private IContainer getContainer(IResource[] clipboardContent) {
		List<? extends IResource> selection = getSelectedResources();

		// selection is copied to itself => copy to parent
		if (clipboardContent != null && areEqualsUnordered(selection, Arrays.asList(clipboardContent))) {
			return selection.get(0).getParent();
		}

		if (selection.get(0) instanceof IFile) {
			return selection.get(0).getParent();
		}
		return (IContainer) selection.get(0);
	}

	/**
	 * The <code>PasteAction</code> implementation of this
	 * <code>SelectionListenerAction</code> method enables this action if
	 * a resource compatible with what is on the clipboard is selected.
	 *
	 * -Clipboard must have IResource or java.io.File
	 * -Projects can always be pasted if they are open
	 * -Workspace folder may not be copied into itself
	 * -Files and folders may be pasted to a single selected folder in open
	 * 	project or multiple selected files in the same folder
	 */
	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		if (!super.updateSelection(selection)) {
			return false;
		}

		IResource[] resourceData = getClipboardResourceData();
		boolean isProjectRes = resourceData != null && resourceData.length > 0
				&& resourceData[0].getType() == IResource.PROJECT;

		if (isProjectRes) {
			for (IResource resource : resourceData) {
				// make sure all resource data are open projects
				// can paste open projects regardless of selection
				if (resource.getType() != IResource.PROJECT || !((IProject) resource).isOpen()) {
					return false;
				}
			}
			return true;
		}

		if (getSelectedNonResources().size() > 0) {
			return false;
		}

		IResource targetResource = getTarget(resourceData);
		// targetResource is null if no valid target is selected (e.g., open project)
		// or selection is empty
		if (targetResource == null) {
			return false;
		}

		// can paste files and folders to a single selection (file, folder,
		// open project) or multiple file/folder selection with the same parent
		List<? extends IResource> selectedResources = getSelectedResources();
		if (selectedResources.size() > 1) {
			if (!selectionIsOfType(IResource.FILE | IResource.FOLDER)) {
				return false;
			}
			for (IResource resource : selectedResources) {
				if (!targetResource.equals(resource.getParent())) {
					return false;
				}
			}
		}
		if (resourceData != null) {
			// linked resources can only be pasted into projects
			if (isLinked(resourceData)
				&& targetResource.getType() != IResource.PROJECT
				&& targetResource.getType() != IResource.FOLDER) {
				return false;
			}
			return true;
		}
		TransferData[] transfers = clipboard.getAvailableTypes();
		FileTransfer fileTransfer = FileTransfer.getInstance();
		for (TransferData transfer : transfers) {
			if (fileTransfer.isSupportedType(transfer)) {
				return true;
			}
		}
		return false;
	}
}