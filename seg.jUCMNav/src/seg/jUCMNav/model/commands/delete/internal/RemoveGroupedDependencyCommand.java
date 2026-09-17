/**
 * 
 */
package seg.jUCMNav.model.commands.delete.internal;

import grl.GroupedDependency;
import org.eclipse.gef.commands.Command;

import seg.jUCMNav.Messages;
import seg.jUCMNav.model.commands.JUCMNavCommand;
import urn.URNspec;

/**
 * Delete a GroupedDependency definition. The definition should have no references.
 * 
 * @author jkealey, pchen
 * 
 */
public class RemoveGroupedDependencyCommand extends Command implements JUCMNavCommand {

    // the grouped dependency to delete
    private GroupedDependency element;

    // the URNspec in which it is contained
    private URNspec urn;

    /**
     * 
     */
    public RemoveGroupedDependencyCommand(GroupedDependency groupedDependency) {
        this.element = groupedDependency;
        setLabel(Messages.getString("RemoveGroupedDependencyCommand.removeGroupedDependency")); //$NON-NLS-1$
    }

    /**
     * Only if not referenced.
     * 
     * @see org.eclipse.gef.commands.Command#canExecute()
     */
    public boolean canExecute() {
        return element != null;
    }

    /**
     * @see org.eclipse.gef.commands.Command#execute()
     */
    public void execute() {
        urn = element.getGrlspec().getUrnspec();

        redo();
    }

    /**
     * @see org.eclipse.gef.commands.Command#redo()
     */
    public void redo() {
        testPreConditions();

        // remove the GroupedDependency from the urnspec
        urn.getGrlspec().getGroupedDependencies().remove(element);

        testPostConditions();
    }

    /*
     * (non-Javadoc)
     * 
     * @see seg.jUCMNav.model.commands.JUCMNavCommand#testPreConditions()
     */
    public void testPreConditions() {
        assert element != null && urn != null : "pre something is null"; //$NON-NLS-1$
        assert urn.getGrlspec().getGroupedDependencies().contains(element) : "pre element in model"; //$NON-NLS-1$

    }

    /*
     * (non-Javadoc)
     * 
     * @see seg.jUCMNav.model.commands.JUCMNavCommand#testPostConditions()
     */
    public void testPostConditions() {
        assert element != null && urn != null : "post something is null"; //$NON-NLS-1$
        assert element.getRefs().size() == 0 : "post can't delete if still referenced."; //$NON-NLS-1$
        assert !urn.getGrlspec().getGroupedDependencies().contains(element) : "post element in model"; //$NON-NLS-1$
    }

    /**
     * 
     * @see org.eclipse.gef.commands.Command#undo()
     */
    public void undo() {
        testPostConditions();

        // re-add grouped dependency
        urn.getGrlspec().getGroupedDependencies().add(element);

        testPreConditions();
    }
}