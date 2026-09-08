package seg.jUCMNav.figures.anchors;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

import seg.jUCMNav.figures.IntentionalElementFigure;

/**
 * This anchor is used for decomposition. It is at the bottom of an intentionalElement
 * 
 * @author Jean-Fran�ois Roy
 * 
 */
public class DecompositionAnchor extends AbstractConnectionAnchor {

    public static final int TYPE_SRC = 0;
    public static final int TYPE_TARGET = 1;

    private int type;

    /**
     * @param owner
     */
    public DecompositionAnchor(IFigure owner, int type) {
        super(owner);
        this.type = type;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.draw2d.ConnectionAnchor#getLocation(org.eclipse.draw2d.geometry.Point)
     */
    public Point getLocation(Point reference) {
        // When this is the decomposition target anchor and the owner node shows a
        // decomposition-type label, the connection should terminate at the label's
        // bottom-center instead of the node's own bottom-center.
        //
        // The label itself lives on the diagram's primary layer (it cannot be a child of the
        // node: children are painted clipped to the node's bounds), so rather than reading the
        // label's bounds -- which live in a different figure tree and may not be laid out when
        // the connection routes -- the point is derived from the node bounds plus the label's
        // measured height. IntentionalElementEditPart positions the label centered directly
        // under the node, its top on the node's bottom border, so the label's bottom-center is
        // (node center x, node bottom + label height) by construction.
        if (this.type == TYPE_TARGET && getOwner() instanceof IntentionalElementFigure) {
            IntentionalElementFigure fig = (IntentionalElementFigure) getOwner();
            Label label = fig.getDecompositionLabel();
            if (label != null && label.isVisible()) {
                Dimension size = label.getPreferredSize();
                Rectangle r = getOwner().getBounds().getCopy();
                Point p = new Point(r.getCenter().x, r.getBottom().y + size.height);
                getOwner().translateToAbsolute(p);
                return p;
            }
        }

        Rectangle r = getOwner().getBounds().getCopy();
        Point p = new Point(r.getCenter().x, this.type == TYPE_TARGET ? r.getBottom().y : r.getTop().y);
        getOwner().translateToAbsolute(p);
        return p;
    }

}
