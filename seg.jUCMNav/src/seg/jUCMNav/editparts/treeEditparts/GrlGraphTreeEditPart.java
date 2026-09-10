package seg.jUCMNav.editparts.treeEditparts;

import fm.FeatureDiagram;
import grl.GRLGraph;
import grl.GRLNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.gef.EditPolicy;
import org.eclipse.swt.graphics.Image;

import seg.jUCMNav.JUCMNavPlugin;
import seg.jUCMNav.editpolicies.element.GRLGraphComponentEditPolicy;
import seg.jUCMNav.model.commands.create.GenerateInstanceModelCommand;
import seg.jUCMNav.model.util.DelegatingElementComparator;


/**
 * Tree edit part for the GrlGraph
 * 
 * @author Jean-Fran�ois Roy, pchen
 * 
 */
public class GrlGraphTreeEditPart extends UrnModelElementTreeEditPart {

    /**
     * @param model
     *            the GrlGraph
     */
    public GrlGraphTreeEditPart(GRLGraph model) {
        super(model);
    }

    /**
     * @see org.eclipse.gef.editparts.AbstractTreeEditPart#createEditPolicies()
     */
    protected void createEditPolicies() {
        installEditPolicy(EditPolicy.COMPONENT_ROLE, new GRLGraphComponentEditPolicy());
    }

    /**
     * Returns list of actorref, beliefs and intentionalElementRef, kpiInformationElementRef sorted by type and name using EObjectClassNameComparator
     * 
     * @see org.eclipse.gef.EditPart#getChildren()
     */
    public List<GRLNode> getModelChildren() {
        ArrayList<GRLNode> list = new ArrayList<GRLNode>();
        GRLGraph graph = getGraph();
        list.addAll(graph.getContRefs());
        Vector<GRLNode> v = new Vector<GRLNode>();
        for (Iterator iter = graph.getNodes().iterator(); iter.hasNext();) {
            GRLNode element = (GRLNode) iter.next();
            v.add(element);
        }
        list.addAll(v);

        Collections.sort(list, new DelegatingElementComparator());
        return list;
    }

    /**
     * 
     * @return the GRLGraph
     */
    public GRLGraph getGraph() {
        return ((GRLGraph) getModel());
    }

    /**
     * Returns an icon representing a GrlGraph. Recomputed on every call so a tree node whose model
     * becomes (or stops being) an instance model picks the right icon instead of latching the first
     * one it ever computed; the images are cached singletons, so identity comparison is free.
     */
    protected Image getImage() {
        Image icon;
        if (getGraph() instanceof FeatureDiagram)
            // FM icon
            icon = JUCMNavPlugin.getImage("icons/fmd16.gif"); //$NON-NLS-1$
        else if (GenerateInstanceModelCommand.isInstanceModel(getGraph()))
            // instance model icon
            icon = JUCMNavPlugin.getImage(JUCMNavPlugin.getInstanceIconDescriptor());
        else
            // GRL icon
            icon = JUCMNavPlugin.getImage("icons/grl16.gif"); //$NON-NLS-1$
        if (super.getImage() == null || super.getImage() != icon)
            setImage(icon);
        return super.getImage();
    }

}
