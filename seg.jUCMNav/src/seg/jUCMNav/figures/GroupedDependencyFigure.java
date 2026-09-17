package seg.jUCMNav.figures;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;

import seg.jUCMNav.views.preferences.GeneralPreferencePage;

/**
 * Figure representing a grouped dependency: a rectangle containing a "D" glyph that points toward the
 * target instances.
 * 
 * @author skuly
 * 
 */
public class GroupedDependencyFigure extends Shape {

    // default sizes: a little wider than the original squish so the D has room to breathe, height back to 80% of the
    // original square
    protected final static int DEFAULT_WIDTH = 34;
    protected final static int DEFAULT_HEIGHT = 40;

    // the D glyph is drawn at most this tall; widening the box does not grow it
    protected final static int MAX_D_RADIUS = 8;

    // minimum gap between the box edge and the multiplicity label on the perpendicular sides
    // (box pointing up or down); the labels above/below a left/right pointing box stay abutted.
    private final static int LABEL_GAP = 2;

    // the side toward which the D points
    public static final int SIDE_RIGHT = 0;
    public static final int SIDE_LEFT = 1;
    public static final int SIDE_TOP = 2;
    public static final int SIDE_BOTTOM = 3;

    /**
     * @return the side opposite to {@code side}.
     */
    public static int oppositeSide(int side) {
        switch (side) {
        case SIDE_LEFT:
            return SIDE_RIGHT;
        case SIDE_TOP:
            return SIDE_BOTTOM;
        case SIDE_BOTTOM:
            return SIDE_TOP;
        case SIDE_RIGHT:
        default:
            return SIDE_LEFT;
        }
    }

    /**
     * @return the side ({@link #SIDE_RIGHT}, {@link #SIDE_LEFT}, {@link #SIDE_TOP} or
     *         {@link #SIDE_BOTTOM}) the multiplicity label orbits to for a box pointing toward
     *         {@code targetSide}: the clockwise-perpendicular side, one of the two sides the fan
     *         connections never attach to.
     */
    public static int labelSideForTargetSide(int targetSide) {
        switch (targetSide) {
        case SIDE_RIGHT:
            return SIDE_TOP;
        case SIDE_TOP:
            return SIDE_RIGHT;
        case SIDE_LEFT:
            return SIDE_BOTTOM;
        case SIDE_BOTTOM:
        default:
            return SIDE_LEFT;
        }
    }

    private int targetSide = SIDE_RIGHT;
    private boolean impossible;

    // the size of the visual box; grows/shrinks with orientation, but is deliberately not forced
    // to the orientation aspect until the box actually flips (see setTargetSide): a placement whose
    // side never changes keeps the size it started with.
    private Dimension boxSize = new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    private final Label multiplicityLabel = new Label();
    private String labelText = ""; //$NON-NLS-1$

    private ConnectionAnchor sideAnchor;
    private ConnectionAnchor sourceAnchor;
    private ConnectionAnchor targetAnchor;

    /**
     * @return Returns the default dimension.
     */
    public static Dimension getDefaultDimension() {
        return new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Constructor of the grouped dependency figure.
     */
    public GroupedDependencyFigure() {
        super();
        setLineWidth(3);
        setFill(true);
        setBackgroundColor(ColorManager.FILL);
        setForegroundColor(ColorManager.LINE);
        setAntialias(GeneralPreferencePage.getAntialiasingPref());

        setPreferredSize(GroupedDependencyFigure.getDefaultDimension());
        setSize(GroupedDependencyFigure.getDefaultDimension());

        // the target multiplicity readout, hidden until a multiplicity is set. draw2d clips every
        // child to the figure's own bounds, so the label lives inside an enlarged strip on the
        // label side and the painted box is inset; see getVisualBox()/layoutLabel().
        multiplicityLabel.setForegroundColor(ColorManager.LINKREFLABEL);
        add(multiplicityLabel);
        multiplicityLabel.setVisible(false);

        sideAnchor = new GroupedDependencySideAnchor(this);
        sourceAnchor = new GroupedDependencyFixedAnchor(this, true);
        targetAnchor = new GroupedDependencyFixedAnchor(this, false);
    }

    /**
     * @return the connection anchor, placed on the side of the figure facing the connection's far end.
     */
    public ConnectionAnchor getConnectionAnchor() {
        return sideAnchor;
    }

    /**
     * @return the anchor for the source end of connections leaving the box (the box-to-target fan):
     *         placed on the side where the D bulges (the target side).
     */
    public ConnectionAnchor getSourceAnchor() {
        return sourceAnchor;
    }

    /**
     * @return the anchor for the target end of connections entering the box (the source-to-box fan):
     *         placed on the side where the spine of the D is (opposite the target side).
     */
    public ConnectionAnchor getTargetAnchor() {
        return targetAnchor;
    }

    /**
     * @return the side ({@link #SIDE_RIGHT}, {@link #SIDE_LEFT}, {@link #SIDE_TOP} or {@link #SIDE_BOTTOM}) toward
     *         which the D points.
     */
    public int getTargetSide() {
        return targetSide;
    }

    /**
     * Sets the side toward which the D points.
     * 
     * @param targetSide
     *            one of {@link #SIDE_RIGHT}, {@link #SIDE_LEFT}, {@link #SIDE_TOP} or {@link #SIDE_BOTTOM}
     */
    public void setTargetSide(int targetSide) {
        if (this.targetSide == targetSide)
            return;
        this.targetSide = targetSide;

        // Let the box "turn" with the D: a vertical orientation reads as a tall box, a horizontal
        // one as a wide box. Swapping the aspect on the flip makes the whole symbol look rotated
        // instead of a portrait rectangle with a sideways D inside. The size is built by
        // recomputeSize() from this new box size plus the label strip, if any.
        boxSize = getOrientationSize(targetSide).getCopy();
        recomputeSize();
        layoutLabel();
        repaint();

        // GEF caches the anchors of the box's fan connections and only re-queries them when the
        // anchors announce they moved, otherwise those connections keep hugging the old side until
        // the box itself is dragged. Fire the notification so every attached connection re-routes
        // against the new target side immediately.
        fireAnchorsMoved();
    }

    /**
     * Sets the target-multiplicity text shown next to the box (rendered e.g. as {@code [1..*]}).
     * An empty/null text hides the label and returns the figure to its plain box size; any other
     * text shows the label on the free side and enlarges the figure by its strip.
     */
    public void setLabelText(String text) {
        String t = text == null ? "" : text; //$NON-NLS-1$
        if (t.equals(labelText))
            return;
        labelText = t;
        multiplicityLabel.setText(labelText);
        multiplicityLabel.setVisible(labelText.length() > 0);
        recomputeSize();
        layoutLabel();
    }

    /**
     * @return the rectangle actually painted (the box, its D/X glyph and the fan attachment
     *         points). When the multiplicity label is hidden this equals the figure bounds; when it
     *         is visible the figure bounds are enlarged by the label strip on the label side and the
     *         visual box is inset by that strip, so the box itself keeps its exact pre-label
     *         position and size.
     */
    public Rectangle getVisualBox() {
        Rectangle r = getBounds().getCopy();
        if (!multiplicityLabel.isVisible())
            return r;
        Dimension labelSize = multiplicityLabel.getPreferredSize();
        switch (labelSideForTargetSide(targetSide)) {
        case SIDE_LEFT:
            r.x += labelSize.width + LABEL_GAP;
            r.width -= labelSize.width + LABEL_GAP;
            break;
        case SIDE_RIGHT:
            r.width -= labelSize.width + LABEL_GAP;
            break;
        case SIDE_TOP:
            r.y += labelSize.height;
            r.height -= labelSize.height;
            break;
        case SIDE_BOTTOM:
        default:
            r.height -= labelSize.height;
            break;
        }
        return r;
    }

    /**
     * Recomputes the figure size from the current visual box size plus, when the label is visible,
     * the label's preferred width (left/right strip) or height (top/bottom strip) on the label side.
     */
    private void recomputeSize() {
        Dimension size = boxSize.getCopy();
        if (multiplicityLabel.isVisible()) {
            Dimension labelSize = multiplicityLabel.getPreferredSize();
            int labelSide = labelSideForTargetSide(targetSide);
            if (labelSide == SIDE_LEFT || labelSide == SIDE_RIGHT)
                size.width += labelSize.width + LABEL_GAP;
            else
                size.height += labelSize.height;
        }
        if (!size.equals(getSize())) {
            setSize(size);
            setPreferredSize(size.getCopy());
        }
    }

    /**
     * Places the label abutted to the visual box edge on the label side (gap 0 above/below a
     * left/right pointing box, {@link #LABEL_GAP} on the perpendicular sides of an up/down pointing
     * box), centered along that edge. The text itself always stays horizontal; only the side it
     * orbits to changes with the box orientation.
     */
    public void layoutLabel() {
        if (!multiplicityLabel.isVisible())
            return;
        Rectangle box = getVisualBox();
        Dimension labelSize = multiplicityLabel.getPreferredSize();
        Rectangle label = new Rectangle(0, 0, labelSize.width, labelSize.height);
        switch (labelSideForTargetSide(targetSide)) {
        case SIDE_LEFT:
            label.x = box.x - labelSize.width - LABEL_GAP;
            label.y = box.y + (box.height - labelSize.height) / 2;
            break;
        case SIDE_RIGHT:
            label.x = box.right() + LABEL_GAP;
            label.y = box.y + (box.height - labelSize.height) / 2;
            break;
        case SIDE_TOP:
            label.y = box.y - labelSize.height;
            label.x = box.x + (box.width - labelSize.width) / 2;
            break;
        case SIDE_BOTTOM:
        default:
            label.y = box.bottom();
            label.x = box.x + (box.width - labelSize.width) / 2;
            break;
        }
        multiplicityLabel.setLocation(label.getLocation());
        multiplicityLabel.setSize(label.getSize());
    }

    /**
     * @return the multiplicity label's bounds translated to absolute diagram coordinates, or
     *         {@code null} when the label is hidden (for double-click hit testing).
     */
    public Rectangle getLabelBounds() {
        if (!multiplicityLabel.isVisible())
            return null;
        Rectangle r = multiplicityLabel.getBounds().getCopy();
        translateToAbsolute(r);
        return r;
    }

    /**
     * The figure is only interactive where it paints: the visual box and (when visible) the
     * multiplicity label itself. The rest of the enlarged strip the label runs in is empty
     * background, so hovering, selecting or dragging there must fall through to the diagram
     * below. (For the left/right pointing boxes whose label sits on the top/bottom strip this
     * also excludes the empty ends of that strip; only the text rect and the box are hot.)
     * 
     * The mouse coordinates arrive in this figure's parent space, the same space
     * {@link #getVisualBox()} is expressed in; the label's own bounds are relative to this
     * figure, so they are shifted by the figure's location before the containment test. The
     * label keeps participating in hit-testing so that double-clicking it still reaches this
     * edit part.
     */
    @Override
    public boolean containsPoint(int x, int y) {
        if (getVisualBox().contains(x, y))
            return true;
        if (multiplicityLabel.isVisible()) {
            Rectangle label = multiplicityLabel.getBounds().getCopy();
            label.translate(getBounds().x, getBounds().y);
            return label.contains(x, y);
        }
        return false;
    }

    /**
     * The box dimensions that go with a given target side: portrait (tall) for the up/down
     * orientations, landscape (wide) for the left/right ones.
     */
    private Dimension getOrientationSize(int targetSide) {
        if (targetSide == SIDE_LEFT || targetSide == SIDE_RIGHT)
            return new Dimension(DEFAULT_HEIGHT, DEFAULT_WIDTH);
        return new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Marks the box as impossible (its target-multiplicity requirement exceeds the number of
     * targets): from then on the glyph drawn inside is an "X" instead of a "D". Orientation keeps
     * working normally, so the box still turns with it; only the glyph changes.
     */
    public void setImpossible(boolean impossible) {
        if (this.impossible == impossible)
            return;
        this.impossible = impossible;
        repaint();
    }

    /**
     * Announces that every connection anchor of this figure moved, forcing the attached fan
     * connections to re-route against the current target side.
     */
    private void fireAnchorsMoved() {
        if (sideAnchor instanceof GroupedDependencySideAnchor)
            ((GroupedDependencySideAnchor) sideAnchor).notifyMoved();
        if (sourceAnchor instanceof GroupedDependencyFixedAnchor)
            ((GroupedDependencyFixedAnchor) sourceAnchor).notifyMoved();
        if (targetAnchor instanceof GroupedDependencyFixedAnchor)
            ((GroupedDependencyFixedAnchor) targetAnchor).notifyMoved();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.draw2d.Shape#outlineShape(org.eclipse.draw2d.Graphics)
     */
    protected void outlineShape(Graphics graphics) {
        Rectangle r = getVisualBox();
        r.x += getLineWidth() / 2;
        r.y += getLineWidth() / 2;
        r.width -= getLineWidth();
        r.height -= getLineWidth();
        graphics.drawRectangle(r);

        if (impossible)
            drawX(graphics);
        else
            drawD(graphics);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.draw2d.Shape#fillShape(org.eclipse.draw2d.Graphics)
     */
    protected void fillShape(Graphics graphics) {
        Rectangle r = getVisualBox();
        r.x += getLineWidth() / 2;
        r.y += getLineWidth() / 2;
        r.width -= getLineWidth();
        r.height -= getLineWidth();
        graphics.fillRectangle(r);
    }

    /**
     * Draws the glyph of an impossible grouped dependency: an "X" centered in the box, spanning the
     * same radius as the D's arc so the two glyphs read the same size. The cross is deliberately
     * rotation invariant: the box still turns with the orientation, but the mark itself never points
     * anywhere.
     */
    private void drawX(Graphics graphics) {
        Rectangle r = getVisualBox();
        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2;
        int radius = Math.min(MAX_D_RADIUS, Math.min(r.width, r.height) / 2 - 4);
        if (radius < 4)
            radius = 4;
        graphics.drawLine(cx - radius, cy - radius, cx + radius, cy + radius);
        graphics.drawLine(cx + radius, cy - radius, cx - radius, cy + radius);
    }

    /**
     * Draws the D glyph inside the box, the belly of the D bulging toward the target side. The spine
     * plus the arc are offset so that the whole D (not just its straight spine) is centered on the box.
     * The arc's outline leaves the spine almost straight at both ends and bulges out strongly through
     * the middle (a superellipse, exponent ~3) rather than being a gentle curve all the way around,
     * the way a printed "D" starts flat off the stem and then curves in hard at the belly.
     */
    private void drawD(Graphics graphics) {
        Rectangle r = getVisualBox();
        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2;
        int radius = Math.min(MAX_D_RADIUS, Math.min(r.width, r.height) / 2 - 4);
        if (radius < 4)
            radius = 4;
        // a little extra outward bulge ("stomach") so the belly is not a perfect semicircle
        int stomach = Math.max(2, radius / 3);
        int width = radius + stomach;
        int halfW = width / 2;

        double angle;
        switch (targetSide) {
        case SIDE_LEFT:
            angle = Math.PI;
            break;
        case SIDE_TOP:
            angle = -Math.PI / 2;
            break;
        case SIDE_BOTTOM:
            angle = Math.PI / 2;
            break;
        case SIDE_RIGHT:
        default:
            angle = 0;
            break;
        }
        double cos = Math.cos(angle), sin = Math.sin(angle);

        // canonical D, spine at x = -halfW, belly tip at x = +halfW, ends at y = +-radius
        PointList points = new PointList();
        int samples = 30;
        for (int i = 0; i <= samples; i++) {
            double p = (double) i / samples;
            double y = -radius + 2 * radius * p;
            double t = Math.abs(y) / radius;
            double x = -halfW + width * Math.pow(1 - t * t * t, 1.0 / 3);
            int px = (int) Math.round(cx + x * cos - y * sin);
            int py = (int) Math.round(cy + x * sin + y * cos);
            points.addPoint(px, py);
        }
        graphics.drawPolyline(points);

        // the straight spine, on the opposite side
        int spineX = -halfW;
        int sx1 = (int) Math.round(cx + spineX * cos + radius * sin);
        int sy1 = (int) Math.round(cy + spineX * sin - radius * cos);
        int sx2 = (int) Math.round(cx + spineX * cos - radius * sin);
        int sy2 = (int) Math.round(cy + spineX * sin + radius * cos);
        graphics.drawLine(sx1, sy1, sx2, sy2);
    }

    /**
     * Anchor that places connections on the side of the figure facing the connection's far end.
     * 
     * @author skuly
     */
    private static class GroupedDependencySideAnchor extends AbstractConnectionAnchor {

        public GroupedDependencySideAnchor(GroupedDependencyFigure owner) {
            super(owner);
        }

        /**
         * Announces that this anchor moved, forcing every connection attached to it to re-route.
         */
        public void notifyMoved() {
            fireAnchorMoved();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.draw2d.ConnectionAnchor#getLocation(org.eclipse.draw2d.geometry.Point)
         */
        public Point getLocation(Point reference) {
            Rectangle r = ((GroupedDependencyFigure) getOwner()).getVisualBox();
            Point center = r.getCenter();
            int dx = reference.x - center.x;
            int dy = reference.y - center.y;
            Point p;
            if (Math.abs(dx) >= Math.abs(dy)) {
                p = new Point(dx >= 0 ? r.right() : r.x, center.y);
            } else {
                p = new Point(center.x, dy >= 0 ? r.bottom() : r.y);
            }
            getOwner().translateToAbsolute(p);
            return p;
        }
    }

    /**
     * Anchor fixed to the middle of one side of the box: the spine side when {@code bulge} is
     * {@code false} (hosting the source-to-box fan, i.e. the target ends of the incoming links) or
     * the bulge side when {@code bulge} is {@code true} (hosting the box-to-target fan, i.e. the
     * source ends of the outgoing links). Which side that is follows the D's orientation.
     *
     * @author skuly
     */
    private static class GroupedDependencyFixedAnchor extends AbstractConnectionAnchor {

        private final boolean bulge;

        public GroupedDependencyFixedAnchor(GroupedDependencyFigure owner, boolean bulge) {
            super(owner);
            this.bulge = bulge;
        }

        /**
         * Announces that this anchor moved, forcing every attached connection to re-route.
         */
        public void notifyMoved() {
            fireAnchorMoved();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.draw2d.ConnectionAnchor#getLocation(org.eclipse.draw2d.geometry.Point)
         */
        public Point getLocation(Point reference) {
            Rectangle r = ((GroupedDependencyFigure) getOwner()).getVisualBox();
            int side = ((GroupedDependencyFigure) getOwner()).getTargetSide();
            if (!bulge)
                side = oppositeSide(side);
            Point p;
            switch (side) {
            case SIDE_LEFT:
                p = new Point(r.x, r.y + r.height / 2);
                break;
            case SIDE_TOP:
                p = new Point(r.x + r.width / 2, r.y);
                break;
            case SIDE_BOTTOM:
                p = new Point(r.x + r.width / 2, r.bottom());
                break;
            case SIDE_RIGHT:
            default:
                p = new Point(r.right(), r.y + r.height / 2);
                break;
            }
            getOwner().translateToAbsolute(p);
            return p;
        }
    }
}