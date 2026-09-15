package seg.jUCMNav.actions;

import grl.GroupedDependency;
import grl.GroupedDependencyLink;
import grl.LinkRef;

import java.util.Iterator;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import seg.jUCMNav.Messages;
import seg.jUCMNav.editparts.GroupedDependencyEditPart;
import seg.jUCMNav.editparts.LinkRefEditPart;
import seg.jUCMNav.model.commands.transformations.ChangeGroupedDependencyTargetMultiplicityCommand;
import seg.jUCMNav.model.util.DependencyMultiplicity;
import seg.jUCMNav.strategies.util.ReusedElementUtil;
import seg.jUCMNav.views.dialogs.MultiplicityDialog;

/**
 * Opens the multiplicity dialog for the target end of the selected grouped dependency (the D box)
 * and executes a {@link ChangeGroupedDependencyTargetMultiplicityCommand} with the resulting value.
 * 
 * <p>
 * The whole construct is the grouped dependency: the action is enabled on a single D box or on a
 * single fan link (a {@link LinkRef} whose definition is a {@link GroupedDependencyLink}), so the
 * target multiplicity can be changed from either.
 * </p>
 * 
 * @author skuly
 */
public class ChangeGroupedDependencyTargetMultiplicityAction extends URNSelectionAction {

    public static final String SET_TARGET_MULTIPLICITY = "seg.jUCMNav.SetGroupedDependencyTargetMultiplicity"; //$NON-NLS-1$

    private GroupedDependency box;

    /**
     * @param part
     */
    public ChangeGroupedDependencyTargetMultiplicityAction(IWorkbenchPart part) {
        super(part);
        setId(SET_TARGET_MULTIPLICITY);
    }

    /**
     * Resolves the grouped dependency box behind a fan link, or {@code null} when the link is not a
     * fan (its definition is not a {@link GroupedDependencyLink}) or the box cannot be found on
     * either end.
     */
    public static GroupedDependency findGroupedDependency(LinkRef linkRef) {
        if (linkRef == null || linkRef.getLink() == null)
            return null;
        if (!(linkRef.getLink() instanceof GroupedDependencyLink))
            return null;
        if (linkRef.getSource() instanceof GroupedDependency)
            return (GroupedDependency) linkRef.getSource();
        if (linkRef.getTarget() instanceof GroupedDependency)
            return (GroupedDependency) linkRef.getTarget();
        return null;
    }

    /**
     * We need exactly one grouped dependency box or one fan link selected.
     */
    protected boolean calculateEnabled() {
        box = null;
        int matches = 0;
        for (Iterator iter = getSelectedObjects().iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof GroupedDependencyEditPart) {
                box = (GroupedDependency) ((GroupedDependencyEditPart) obj).getModel();
            } else if (obj instanceof LinkRefEditPart) {
                LinkRef linkRef = ((LinkRefEditPart) obj).getLinkRef();
                if (ReusedElementUtil.isReuseLink(linkRef.getLink()))
                    return false;
                box = findGroupedDependency(linkRef);
                if (box == null)
                    return false;
            } else {
                return false;
            }
            matches++;
        }
        return matches == 1 && box != null;
    }

    public void run() {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        MultiplicityDialog dialog = new MultiplicityDialog(shell, DependencyMultiplicity.normalizeStored(box.getDestMultiplicity()),
                Messages.getString("MultiplicityDialog.titleTarget"), //$NON-NLS-1$
                Messages.getString("MultiplicityDialog.labelTarget")); //$NON-NLS-1$
        if (dialog.open() == IDialogConstants.OK_ID)
            execute(new ChangeGroupedDependencyTargetMultiplicityCommand(box, dialog.getValue()));
    }
}