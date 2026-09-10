package seg.jUCMNav.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import grl.GRLGraph;
import seg.jUCMNav.JUCMNavPlugin;
import seg.jUCMNav.Messages;
import seg.jUCMNav.editors.UCMNavMultiPageEditor;
import seg.jUCMNav.model.commands.create.GenerateInstanceModelCommand;

/**
 * Context-menu action shown on instance models: jumps to the type-model graph the selected
 * instance model was generated from.
 * 
 * <p>
 * Instance and type models live in the same URNspec, so both are pages of the same editor; the
 * action simply activates the right page.
 * </p>
 * 
 * @author skuly
 */
public class GoToTypeModelAction extends URNSelectionAction {

    public static final String GOTOTYPEMODEL = "seg.jUCMNav.GoToTypeModel"; //$NON-NLS-1$

    public GoToTypeModelAction(IWorkbenchPart part) {
        super(part);
        setId(GOTOTYPEMODEL);
        setImageDescriptor(JUCMNavPlugin.getImageDescriptor("icons/grl16.gif")); //$NON-NLS-1$
        setText(Messages.getString("ActionRegistryManager.goToTypeModel")); //$NON-NLS-1$
    }

    /**
     * Only shown when a GRL graph that is an instance model is selected (diagram background,
     * like the generate action), and its type model can be resolved.
     */
    protected boolean calculateEnabled() {
        SelectionHelper sel = new SelectionHelper(getSelectedObjects());
        if (sel.getUrnspec() == null || sel.getSelectionType() != SelectionHelper.GRLGRAPH)
            return false;
        return GenerateInstanceModelCommand.getTypeModel(sel.getUrnspec(), sel.getGrlgraph()) != null;
    }

    /**
     * @see org.eclipse.jface.action.IAction#run()
     */
    public void run() {
        SelectionHelper sel = new SelectionHelper(getSelectedObjects());
        if (sel.getUrnspec() == null)
            return;
        GRLGraph graph = sel.getGrlgraph();
        if (graph == null)
            return;
        GRLGraph typeModel = GenerateInstanceModelCommand.getTypeModel(sel.getUrnspec(), graph);
        if (typeModel == null) {
            openError(Messages.getString("GoToTypeModelAction.typeModelMissing")); //$NON-NLS-1$
            return;
        }
        UCMNavMultiPageEditor editor = getEditor();
        if (editor != null)
            editor.setActivePage(typeModel);
    }

    private Shell getShell() {
        if (PlatformUI.getWorkbench() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null)
            return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        return null;
    }

    private void openError(String message) {
        Shell shell = getShell();
        if (shell != null)
            MessageDialog.openError(shell, Messages.getString("GoToTypeModelAction.title"), message); //$NON-NLS-1$
    }
}