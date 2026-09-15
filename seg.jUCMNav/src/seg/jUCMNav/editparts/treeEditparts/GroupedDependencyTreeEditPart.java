package seg.jUCMNav.editparts.treeEditparts;

import grl.GroupedDependency;

import org.eclipse.gef.EditPolicy;
import org.eclipse.swt.graphics.Image;

import seg.jUCMNav.JUCMNavPlugin;
import seg.jUCMNav.editpolicies.element.GRLNodeComponentEditPolicy;

/**
 * TreeEditPart for the grouped dependency boxes (the D boxes generated in instance models).
 * 
 * @author skuly
 */
public class GroupedDependencyTreeEditPart extends UrnModelElementTreeEditPart {

    /**
     * @param model
     *            the grouped dependency box
     */
    public GroupedDependencyTreeEditPart(GroupedDependency model) {
        super(model);
    }

    /**
     * @see org.eclipse.gef.editparts.AbstractEditPart#createEditPolicies()
     */
    protected void createEditPolicies() {
        installEditPolicy(EditPolicy.COMPONENT_ROLE, new GRLNodeComponentEditPolicy());
    }

    /**
     * Returns an image representing the grouped dependency.
     */
    protected Image getImage() {
        if (super.getImage() == null) {
            setImage(JUCMNavPlugin.getImage("icons/Node16.gif")); //$NON-NLS-1$
        }
        return super.getImage();
    }
}