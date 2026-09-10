/**
 * 
 */
package seg.jUCMNav.actions;

import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import grl.ActorRef;
import grl.GRLGraph;
import seg.jUCMNav.JUCMNavPlugin;
import seg.jUCMNav.Messages;
import seg.jUCMNav.editors.UCMNavMultiPageEditor;
import seg.jUCMNav.model.commands.create.GenerateInstanceModelCommand;
import seg.jUCMNav.model.commands.create.GenerateInstanceModelCommand.GenerationProblem;
import seg.jUCMNav.views.dialogs.GenerateInstanceModelDialog;

/**
 * Context-menu action that instantiates the selected GRL graph: asks how many copies of each
 * actor must be generated and runs {@link GenerateInstanceModelCommand}, which produces a new,
 * independent GRL graph (an instance model of the type model being edited).
 * 
 * <p>
 * If the graph contains elements that are not supported in type/instance models (nested actors,
 * free-floating intentional elements) or links whose instantiation is work in progress
 * (dependencies between different actors), the action shows an error dialog and aborts.
 * </p>
 * 
 * @author skuly
 */
public class GenerateInstanceModelAction extends URNSelectionAction {

    public static final String GENERATEINSTANCEMODEL = "seg.jUCMNav.GenerateInstanceModel"; //$NON-NLS-1$

    public GenerateInstanceModelAction(IWorkbenchPart part) {
        super(part);
        setId(GENERATEINSTANCEMODEL);
        setImageDescriptor(JUCMNavPlugin.getInstanceIconDescriptor());
        setText(Messages.getString("ActionRegistryManager.generateInstanceModel")); //$NON-NLS-1$
    }

    /**
     * Only shown when a GRL graph itself is selected (diagram background), never on the context
     * menu of an element inside the graph. It stays enabled even if the graph cannot be
     * instantiated: clicking it then explains the problem instead of silently hiding the feature.
     * The sole exception are instance models, which cannot be instantiated at all.
     */
    protected boolean calculateEnabled() {
        SelectionHelper sel = new SelectionHelper(getSelectedObjects());
        if (sel.getUrnspec() == null || sel.getSelectionType() != SelectionHelper.GRLGRAPH)
            return false;
        return !GenerateInstanceModelCommand.isInstanceModel(sel.getGrlgraph());
    }

    /**
     * Opens the count dialog, then generates the instance model. Unsupported elements abort the
     * action with an error dialog before any change is made.
     * 
     * @see org.eclipse.jface.action.IAction#run()
     */
    public void run() {
        SelectionHelper sel = new SelectionHelper(getSelectedObjects());
        if (sel.getUrnspec() == null)
            return;
        GRLGraph graph = sel.getGrlgraph();
        if (graph == null)
            return;

        String problemMessage = messageFor(GenerateInstanceModelCommand.analyze(graph));
        if (problemMessage != null) {
            openError(problemMessage);
            return;
        }

        Shell shell = getShell();
        if (shell == null)
            return;

        GenerateInstanceModelDialog dialog = new GenerateInstanceModelDialog(shell, graph);
        if (dialog.open() != GenerateInstanceModelDialog.OK)
            return;

        Map<ActorRef, Integer> counts = dialog.getCounts();
        int margin = dialog.getMargin();

        GenerateInstanceModelCommand command = new GenerateInstanceModelCommand(sel.getUrnspec(), graph, counts, margin);
        if (!command.canExecute()) {
            openError(Messages.getString("GenerateInstanceModelCommand.problemNoActor")); //$NON-NLS-1$
            return;
        }

        UCMNavMultiPageEditor editor = getEditor();
        if (editor != null)
            editor.getDelegatingCommandStack().execute(command);
        else
            command.execute();

        if (editor != null)
            editor.setActivePage(command.getDiagram());
    }

    private String messageFor(GenerationProblem problem) {
        switch (problem) {
        case NESTED_ACTOR:
            return Messages.getString("GenerateInstanceModelCommand.problemNested"); //$NON-NLS-1$
        case FREE_FLOATING_IE:
            return Messages.getString("GenerateInstanceModelCommand.problemFreeFloating"); //$NON-NLS-1$
        case DEPENDENCY:
            return Messages.getString("GenerateInstanceModelCommand.problemDependency"); //$NON-NLS-1$
        case NO_ACTOR:
            return Messages.getString("GenerateInstanceModelCommand.problemNoActor"); //$NON-NLS-1$
        default:
            return null;
        }
    }

    private Shell getShell() {
        if (PlatformUI.getWorkbench() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null)
            return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        return null;
    }

    private void openError(String message) {
        Shell shell = getShell();
        if (shell != null)
            MessageDialog.openError(shell, Messages.getString("GenerateInstanceModelDialog.title"), message); //$NON-NLS-1$
    }
}