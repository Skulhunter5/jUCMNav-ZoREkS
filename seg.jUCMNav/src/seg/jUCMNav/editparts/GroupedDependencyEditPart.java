package seg.jUCMNav.editparts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import grl.GroupedDependency;
import grl.GroupedDependencyRef;
import grl.LinkRef;
import seg.jUCMNav.Messages;
import seg.jUCMNav.editpolicies.element.GRLNodeComponentEditPolicy;
import seg.jUCMNav.editpolicies.feedback.GrlNodeFeedbackEditPolicy;
import seg.jUCMNav.figures.GroupedDependencyFigure;
import seg.jUCMNav.model.commands.transformations.ChangeGroupedDependencyOrientationOverrideCommand;
import seg.jUCMNav.model.commands.transformations.ChangeGroupedDependencyTargetMultiplicityCommand;
import seg.jUCMNav.model.util.DependencyMultiplicity;
import seg.jUCMNav.model.util.MetadataHelper;
import seg.jUCMNav.views.dialogs.MultiplicityDialog;
import urncore.IURNConnection;
import urncore.IURNNode;
import urncore.Metadata;

/**
 * EditPart for a {@link GroupedDependency}. Draws a box with a D glyph pointing toward the target instances.
 * 
 * @author skuly
 * 
 */
public class GroupedDependencyEditPart extends GrlNodeEditPart implements NodeEditPart {

    private final Set farEndAdapters = new HashSet();
    private boolean syncingFarEndAdapters;
    private final Set metadataAdapters = new HashSet();
    private boolean syncingMetadataAdapters;
    private boolean wasImpossible;

    /**
     * @param model
     *            the grouped dependency box to draw
     */
    public GroupedDependencyEditPart(GroupedDependencyRef model) {
        super();
        setModel(model);
    }

    /**
     * Create the edit policies.
     * 
     * @see seg.jUCMNav.editparts.ModelElementEditPart#createEditPolicies()
     */
    protected void createEditPolicies() {
        installEditPolicy(EditPolicy.COMPONENT_ROLE, new GRLNodeComponentEditPolicy());
        installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new GrlNodeFeedbackEditPolicy());
    }

    /**
     * Create the grouped dependency figure.
     * 
     * @see seg.jUCMNav.editparts.ModelElementEditPart#createFigure()
     */
    protected IFigure createFigure() {
        return new GroupedDependencyFigure();
    }

    /**
     * @return the node's figure
     */
    public GroupedDependencyFigure getNodeFigure() {
        return (GroupedDependencyFigure) getFigure();
    }

    /**
     * @return the grouped dependency box (a {@link GroupedDependencyRef}).
     */
    private GroupedDependencyRef getNode() {
        return (GroupedDependencyRef) getModel();
    }

    /**
     * @return the grouped dependency definition behind the box.
     */
    private GroupedDependency getDef() {
        return getNode().getDef();
    }

    /**
     * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#getModelSourceConnections()
     */
    protected List getModelSourceConnections() {
        return getNode().getSucc();
    }

    /**
     * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#getModelTargetConnections()
     */
    protected List getModelTargetConnections() {
        return getNode().getPred();
    }

    /**
     * @see org.eclipse.gef.NodeEditPart#getSourceConnectionAnchor(org.eclipse.gef.ConnectionEditPart)
     */
    public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connection) {
        updateTargetOrientation();
        return getNodeFigure().getSourceAnchor();
    }

    /**
     * @see org.eclipse.gef.NodeEditPart#getSourceConnectionAnchor(org.eclipse.gef.Request)
     */
    public ConnectionAnchor getSourceConnectionAnchor(Request request) {
        return getNodeFigure().getSourceAnchor();
    }

    /**
     * @see org.eclipse.gef.NodeEditPart#getTargetConnectionAnchor(org.eclipse.gef.ConnectionEditPart)
     */
    public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connection) {
        updateTargetOrientation();
        return getNodeFigure().getTargetAnchor();
    }

    /**
     * @see org.eclipse.gef.NodeEditPart#getTargetConnectionAnchor(org.eclipse.gef.Request)
     */
    public ConnectionAnchor getTargetConnectionAnchor(Request request) {
        return getNodeFigure().getTargetAnchor();
    }

    /**
     * Double-click on the box opens the multiplicity dialog for its target end; the resulting value
     * is applied to the grouped dependency itself via the command stack. F2 (direct edit) is a
     * no-op: delegating it to {@link GrlNodeEditPart} would cast this non-{@code GrlNodeFigure}
     * figure in its direct-edit path. All other requests still go to super.
     * 
     * @see seg.jUCMNav.editparts.ModelElementEditPart#performRequest(org.eclipse.gef.Request)
     */
    public void performRequest(Request req) {
        if (req.getType() == RequestConstants.REQ_OPEN) {
            openMultiplicityDialog();
            return;
        }
        if (req.getType() == RequestConstants.REQ_DIRECT_EDIT)
            return;
        super.performRequest(req);
    }

    private void openMultiplicityDialog() {
        String current = getDef().getDestMultiplicity();
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        MultiplicityDialog dialog = new MultiplicityDialog(shell, DependencyMultiplicity.normalizeStored(current),
                Messages.getString("MultiplicityDialog.titleTarget"), //$NON-NLS-1$
                Messages.getString("MultiplicityDialog.labelTarget")); //$NON-NLS-1$
        if (dialog.open() == IDialogConstants.OK_ID) {
            ChangeGroupedDependencyTargetMultiplicityCommand command = new ChangeGroupedDependencyTargetMultiplicityCommand(
                    getDef(), dialog.getValue());
            getViewer().getEditDomain().getCommandStack().execute(command);
        }
    }

    protected void refreshSourceConnections() {
        super.refreshSourceConnections();
        syncFarEndAdapters();
    }

    /**
     * @see org.eclipse.gef.editparts.AbstractEditPart#refreshTargetConnections()
     */
    protected void refreshTargetConnections() {
        super.refreshTargetConnections();
        syncFarEndAdapters();
    }

    /**
     * @see org.eclipse.gef.editparts.AbstractEditPart#activate()
     */
    public void activate() {
        super.activate();
        syncFarEndAdapters();
        syncMetadataAdapters();
    }

    /**
     * @see org.eclipse.gef.editparts.AbstractEditPart#deactivate()
     */
    public void deactivate() {
        unregisterFarEndAdapters();
        unregisterMetadataAdapters();
        super.deactivate();
    }

    /**
     * The box's auto-orientation only needs to hear about moves of the instances on the far end of its
     * fan links (the goals/pages behind the D). Registers this edit part as an EMF adapter on those far
     * instances so that dragging one of them re-runs {@link #updateTargetOrientation()} right away; the
     * membership is recomputed whenever the fan connections change.
     */
    private void syncFarEndAdapters() {
        if (syncingFarEndAdapters)
            return;
        syncingFarEndAdapters = true;
        try {
            Set desired = new HashSet();
            for (Iterator it = getNode().getPred().iterator(); it.hasNext();) {
                Object o = it.next();
                if (o instanceof LinkRef && ((LinkRef) o).getSource() instanceof IURNNode)
                    desired.add(((LinkRef) o).getSource());
            }
            for (Iterator it = getNode().getSucc().iterator(); it.hasNext();) {
                Object o = it.next();
                if (o instanceof LinkRef && ((LinkRef) o).getTarget() instanceof IURNNode)
                    desired.add(((LinkRef) o).getTarget());
            }
            if (desired.equals(farEndAdapters))
                return;
            for (Iterator it = new ArrayList(farEndAdapters).iterator(); it.hasNext();) {
                IURNNode node = (IURNNode) it.next();
                if (!desired.contains(node))
                    node.eAdapters().remove(this);
            }
            farEndAdapters.clear();
            for (Iterator it = desired.iterator(); it.hasNext();) {
                IURNNode node = (IURNNode) it.next();
                node.eAdapters().add(this);
                farEndAdapters.add(node);
            }
        } finally {
            syncingFarEndAdapters = false;
        }
    }

    private void unregisterFarEndAdapters() {
        if (syncingFarEndAdapters)
            return;
        syncingFarEndAdapters = true;
        try {
            for (Iterator it = new ArrayList(farEndAdapters).iterator(); it.hasNext();) {
                IURNNode node = (IURNNode) it.next();
                node.eAdapters().remove(this);
            }
            farEndAdapters.clear();
        } finally {
            syncingFarEndAdapters = false;
        }
    }

    /**
     * The box orientation override is stored as element metadata on the grouped dependency definition.
     * Flipping an existing override to another one fires the EMF notification on that {@link Metadata}
     * child (its value), not on the grouped dependency, so by itself the edit part would never hear
     * about an override-to-override switch. Registering as an adapter on the definition's metadata children,
     * recomputed whenever that list changes, closes the gap; the refresh in
     * {@link #notifyChanged(Notification)} then re-runs {@link #updateTargetOrientation()}.
     */
    private void syncMetadataAdapters() {
        if (syncingMetadataAdapters)
            return;
        syncingMetadataAdapters = true;
        try {
            Set desired = new HashSet(getDef().getMetadata());
            if (desired.equals(metadataAdapters))
                return;
            for (Iterator it = new ArrayList(metadataAdapters).iterator(); it.hasNext();) {
                Metadata metadata = (Metadata) it.next();
                if (!desired.contains(metadata))
                    metadata.eAdapters().remove(this);
            }
            metadataAdapters.clear();
            for (Iterator it = desired.iterator(); it.hasNext();) {
                Metadata metadata = (Metadata) it.next();
                metadata.eAdapters().add(this);
                metadataAdapters.add(metadata);
            }
        } finally {
            syncingMetadataAdapters = false;
        }
    }

    private void unregisterMetadataAdapters() {
        if (syncingMetadataAdapters)
            return;
        syncingMetadataAdapters = true;
        try {
            for (Iterator it = new ArrayList(metadataAdapters).iterator(); it.hasNext();) {
                Metadata metadata = (Metadata) it.next();
                metadata.eAdapters().remove(this);
            }
            metadataAdapters.clear();
        } finally {
            syncingMetadataAdapters = false;
        }
    }

    /**
     * Refreshes the box after any model change.
     * 
     * @see seg.jUCMNav.editparts.ModelElementEditPart#notifyChanged(org.eclipse.emf.common.notify.Notification)
     */
    public void notifyChanged(Notification notification) {
        if (getParent() == null)
            return;
        syncMetadataAdapters();
        refreshTargetConnections();
        refreshSourceConnections();
        refreshVisuals();
        if (notification.getEventType() == Notification.SET && getParent() != null)
            ((URNDiagramEditPart) getParent()).notifyChanged(notification);
    }

    /**
     * Refresh the figure.
     * 
     * @see seg.jUCMNav.editparts.ModelElementEditPart#refreshVisuals()
     */
    protected void refreshVisuals() {
        GroupedDependencyRef node = getNode();
        Point location = new Point(node.getX(), node.getY());
        Dimension size = getNodeFigure().getSize().getCopy();
        Rectangle bounds = new Rectangle(location, size);
        getFigure().setBounds(bounds);
        getFigure().setLocation(location);

        boolean impossible = DependencyMultiplicity.isUnsatisfiable(getDef().getDestMultiplicity());
        getNodeFigure().setImpossible(impossible);
        if (impossible != wasImpossible) {
            // the fan connections decide their own visibility from the box; nudge them so a switch
            // between impossible (X, hidden target fans) and satisfiable (D, visible fans) is
            // reflected right away instead of only at their creation.
            wasImpossible = impossible;
            refreshFanLinkVisibility();
        }

        updateTargetOrientation();

        getFigure().validate();
    }

    /**
     * Re-runs {@link EditPart#refresh()} on every fan connection of the box so each one re-reads
     * whether its figure is hidden (the box-to-target fans of an impossible box are not drawn).
     */
    private void refreshFanLinkVisibility() {
        for (Iterator it = getSourceConnections().iterator(); it.hasNext();)
            ((EditPart) it.next()).refresh();
        for (Iterator it = getTargetConnections().iterator(); it.hasNext();)
            ((EditPart) it.next()).refresh();
    }

    /**
     * The D box points toward the average position of the target instances (the far end of its successor fan links)
     * relative to the average of the source instances (the far end of its predecessor fan links). Called whenever a fan
     * connection re-anchors so the glyph follows goal moves. A manual override stored as element metadata wins over
     * this automatic direction.
     */
    private void updateTargetOrientation() {
        if (getFigure() == null || getNode().getPred() == null || getNode().getSucc() == null)
            return;

        int override = overrideSide(MetadataHelper.getMetaData(getDef(), ChangeGroupedDependencyOrientationOverrideCommand.ORIENTATION_OVERRIDE_KEY));
        if (override >= 0) {
            getNodeFigure().setTargetSide(override);
            return;
        }

        int sourceX = 0, sourceY = 0, sourceCount = 0;
        for (Iterator it = getNode().getPred().iterator(); it.hasNext();) {
            IURNConnection connection = (IURNConnection) it.next();
            if (connection instanceof LinkRef) {
                IURNNode src = connection.getSource();
                if (src != null) {
                    sourceX += src.getX();
                    sourceY += src.getY();
                    sourceCount++;
                }
            }
        }

        int targetX, targetY, targetCount;
        if (DependencyMultiplicity.isUnsatisfiable(getDef().getDestMultiplicity())) {
            // Impossible box: its target fan links are drawn invisible, so base the direction on
            // the hidden targets and it would flip arbitrarily in the empty space the user sees.
            // Orient from the visible sources only: the target side faces away from the sources
            // (toward the box's own other side), which is the only visible geometry to honor.
            targetX = getNode().getX();
            targetY = getNode().getY();
            targetCount = 1;
        } else {
            targetX = 0;
            targetY = 0;
            targetCount = 0;
            for (Iterator it = getNode().getSucc().iterator(); it.hasNext();) {
                IURNConnection connection = (IURNConnection) it.next();
                if (connection instanceof LinkRef) {
                    IURNNode dest = connection.getTarget();
                    if (dest != null) {
                        targetX += dest.getX();
                        targetY += dest.getY();
                        targetCount++;
                    }
                }
            }
        }

        if (sourceCount == 0 || targetCount == 0)
            return;

        int dx = targetX / targetCount - sourceX / sourceCount;
        int dy = targetY / targetCount - sourceY / sourceCount;

        if (Math.abs(dx) >= Math.abs(dy)) {
            getNodeFigure().setTargetSide(dx >= 0 ? GroupedDependencyFigure.SIDE_RIGHT : GroupedDependencyFigure.SIDE_LEFT);
        } else {
            getNodeFigure().setTargetSide(dy >= 0 ? GroupedDependencyFigure.SIDE_BOTTOM : GroupedDependencyFigure.SIDE_TOP);
        }
    }

    /**
     * Maps an override metadata value to a {@link GroupedDependencyFigure} side, or -1 when there is
     * no override (none set, or explicitly cleared).
     */
    private int overrideSide(String value) {
        if (value == null || ChangeGroupedDependencyOrientationOverrideCommand.NONE.equals(value))
            return -1;
        if (ChangeGroupedDependencyOrientationOverrideCommand.RIGHT.equals(value))
            return GroupedDependencyFigure.SIDE_RIGHT;
        if (ChangeGroupedDependencyOrientationOverrideCommand.LEFT.equals(value))
            return GroupedDependencyFigure.SIDE_LEFT;
        if (ChangeGroupedDependencyOrientationOverrideCommand.TOP.equals(value))
            return GroupedDependencyFigure.SIDE_TOP;
        if (ChangeGroupedDependencyOrientationOverrideCommand.BOTTOM.equals(value))
            return GroupedDependencyFigure.SIDE_BOTTOM;
        return -1;
    }
}