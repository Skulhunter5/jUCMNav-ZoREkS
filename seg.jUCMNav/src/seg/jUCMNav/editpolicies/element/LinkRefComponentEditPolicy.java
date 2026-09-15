/**
 * 
 */
package seg.jUCMNav.editpolicies.element;

import grl.GroupedDependency;
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
        // (the box, all its fan links and the shared dependency definition).
        if (linkref.getSource() instanceof GroupedDependency) {
            return new DeleteGRLNodeCommand((GroupedDependency) linkref.getSource());
        } else if (linkref.getTarget() instanceof GroupedDependency) {
            return new DeleteGRLNodeCommand((GroupedDependency) linkref.getTarget());
        }

        return new DeleteLinkRefCommand(linkref);
    }

}
