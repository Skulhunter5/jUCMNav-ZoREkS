package seg.jUCMNav.tests.figures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.junit.Test;

import seg.jUCMNav.figures.IntentionalElementFigure;

/**
 * Regression test for the decomposition-arrow work: the decomposition target
 * anchor of a GRL parent goal must terminate at the bottom-center of the
 * decomposition-type label (AND/OR/XOR) hanging directly underneath the node,
 * and never at the diagram origin.
 *
 * Reported symptom while the label was being moved onto the parent goal node:
 * the arrows pointed at the top-left ("0,0") of the canvas -- reproducibly,
 * because the label was first placed with node-relative offsets, but a GRL node
 * figure does not use local coordinates, so a hand-placed child's bounds live
 * in the node's *parent* space and a "relative" offset of ~(45,43) actually
 * sat at diagram origin (45,43). A later variant positioned the label below the
 * node but inside the figure tree; that painted nothing because the layer
 * clips each child to its own bounds, so a node can never draw below itself.
 *
 * The label is therefore owned by the editor (added to the diagram's primary
 * layer like the evaluation labels), and the anchor derives the connection's
 * end point from the node bounds plus the label's measured height -- the exact
 * spot the editor puts the label -- rather than from a figure it cannot reach.
 *
 * The test is figure-level: it builds the node, positions it well away from
 * the origin, attaches a visible decomposition label, and asserts (a) the
 * target anchor returns (node center x, node bottom + label height), (b) that
 * point is far from the origin, and (c) the non-label fallback returns the
 * node's own bottom-center.
 *
 * @author opencode (decomposition-arrow work)
 */
public class DecompositionAnchorLocationTest {

    private static final Rectangle PARENT_BOUNDS = new Rectangle(500, 300, 120, 50);

    @Test
    public void targetAnchorTracksLabelBottomCenter() {
        IntentionalElementFigure parent = new IntentionalElementFigure();
        parent.setBounds(PARENT_BOUNDS);

        Label label = new Label("And"); //$NON-NLS-1$
        label.setOpaque(true);
        label.setVisible(true);
        parent.setDecompositionLabel(label);

        assertTrue("decomposition label must record a visible label", parent.getDecompositionLabel().isVisible()); //$NON-NLS-1$

        // The label hangs centered under the node, its top touching the bottom border;
        // its bottom-center is (node center x, node bottom + label height), and the
        // anchor must target exactly that.
        Dimension labelSize = label.getPreferredSize();
        Point expected = new Point(PARENT_BOUNDS.x + PARENT_BOUNDS.width / 2,
                PARENT_BOUNDS.y + PARENT_BOUNDS.height + labelSize.height);
        parent.translateToAbsolute(expected);

        Point actual = parent.getDecompositionTarget().getLocation(new Point(0, 0));

        assertEquals("decomposition anchor x diverges from label bottom-center", expected.x, actual.x, 1); //$NON-NLS-1$
        assertEquals("decomposition anchor y diverges from label bottom-center", expected.y, actual.y, 1); //$NON-NLS-1$

        // The node sits right of x=400 / below y=200 -- an origin-anchored point
        // (the pre-fix node-relative placement) is unmistakable here.
        assertTrue("anchor fell back to the diagram origin: " + actual, actual.x > 400 && actual.y > 200); //$NON-NLS-1$
    }

    @Test
    public void anchorFallsBackToNodeBottomWhenLabelHidden() {
        // A regular goal (no decomposition children) has no label: the anchor must
        // return the node's own bottom-center, still far from the origin.
        IntentionalElementFigure parent = new IntentionalElementFigure();
        parent.setBounds(PARENT_BOUNDS);

        assertNull("a non-decomposition parent must carry no label", parent.getDecompositionLabel()); //$NON-NLS-1$

        Point expected = new Point(PARENT_BOUNDS.x + PARENT_BOUNDS.width / 2, PARENT_BOUNDS.y + PARENT_BOUNDS.height);
        parent.translateToAbsolute(expected);

        Point actual = parent.getDecompositionTarget().getLocation(new Point(0, 0));

        assertEquals("fallback anchor x diverges from node bottom-center", expected.x, actual.x, 1); //$NON-NLS-1$
        assertEquals("fallback anchor y diverges from node bottom-center", expected.y, actual.y, 1); //$NON-NLS-1$
    }
}