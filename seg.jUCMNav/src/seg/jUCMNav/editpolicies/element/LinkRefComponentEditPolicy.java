/**
 * 
 */
package seg.jUCMNav.editpolicies.element;

import grl.GroupedDependencyRef;
import grl.LinkRef;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.GroupRequest;

import seg.jUCMNav.model.commands.delete.DeleteGRLNodeCommand;
import seg.jUCMNav.model.commands.delete.DeleteLinkRefCommand;

/**
 * ComponentEditPolicy for LinkRef. Return the command to delete a LinkRef
 * 
 * @author Jean-Fran�ois Roy
 * 
 */
public class LinkRefComponentEditPolicy extends ComponentEditPolicy {

    /**
     * 
     * @see org.eclipse.gef.editpolicies.ComponentEditPolicy#getDeleteCommand(org.eclipse.gef.requests.GroupRequest)
     */
    protected Command getDeleteCommand(GroupRequest request) {

        LinkRef linkref = (LinkRef) getHost().getModel();

        // Deleting one of the fan links of a grouped dependency deletes the whole group
        // (the box, all its fan links and the grouped dependency definition).
        if (linkref.getSource() instanceof GroupedDependencyRef) {
            return new DeleteGRLNodeCommand((GroupedDependencyRef) linkref.getSource());
        } else if (linkref.getTarget() instanceof GroupedDependencyRef) {
            return new DeleteGRLNodeCommand((GroupedDependencyRef) linkref.getTarget());
        }

        return new DeleteLinkRefCommand(linkref);
    }

}
