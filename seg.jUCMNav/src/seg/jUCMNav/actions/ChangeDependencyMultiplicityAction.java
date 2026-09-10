package seg.jUCMNav.actions;

import grl.Dependency;
import grl.LinkRef;

import java.util.Iterator;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import seg.jUCMNav.editparts.LinkRefEditPart;
import seg.jUCMNav.model.commands.transformations.ChangeDependencyMultiplicityCommand;
import seg.jUCMNav.model.util.DependencyMultiplicity;
import seg.jUCMNav.strategies.util.ReusedElementUtil;
import seg.jUCMNav.views.dialogs.MultiplicityDialog;

/**
 * Opens the multiplicity dialog for the source or target end of the selected GRL dependency and
 * executes a {@link ChangeDependencyMultiplicityCommand} with the resulting value.
 * 
 * @author skuly
 */
public class ChangeDependencyMultiplicityAction extends URNSelectionAction {

    public static final String SET_SOURCE_MULTIPLICITY = "seg.jUCMNav.SetSourceMultiplicity"; //$NON-NLS-1$
    public static final String SET_TARGET_MULTIPLICITY = "seg.jUCMNav.SetTargetMultiplicity"; //$NON-NLS-1$

    private int whichEnd;
    private Dependency dependency;

    /**
     * @param part
     * @param whichEnd
     *            {@link ChangeDependencyMultiplicityCommand#SOURCE} or
     *            {@link ChangeDependencyMultiplicityCommand#TARGET}
     */
    public ChangeDependencyMultiplicityAction(IWorkbenchPart part, int whichEnd) {
        super(part);
        this.whichEnd = whichEnd;
        setId(whichEnd == ChangeDependencyMultiplicityCommand.TARGET ? SET_TARGET_MULTIPLICITY : SET_SOURCE_MULTIPLICITY);
    }

    /**
     * We need exactly one dependency (not a reuse link) selected.
     */
    protected boolean calculateEnabled() {
        dependency = null;
        int matches = 0;
        for (Iterator iter = getSelectedObjects().iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (!(obj instanceof LinkRefEditPart))
                return false;

            LinkRef lr = (LinkRef) (((LinkRefEditPart) obj).getModel());
            if (!(lr.getLink() instanceof Dependency) || ReusedElementUtil.isReuseLink(lr.getLink()))
                return false;
            matches++;
            dependency = (Dependency) lr.getLink();
        }
        return matches == 1 && dependency != null;
    }

    public void run() {
        String current = whichEnd == ChangeDependencyMultiplicityCommand.TARGET ? dependency.getDestMultiplicity()
                : dependency.getSrcMultiplicity();

        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        MultiplicityDialog dialog = new MultiplicityDialog(shell, DependencyMultiplicity.normalizeStored(current));
        if (dialog.open() == IDialogConstants.OK_ID)
            execute(new ChangeDependencyMultiplicityCommand(dependency, whichEnd, dialog.getValue()));
    }
}