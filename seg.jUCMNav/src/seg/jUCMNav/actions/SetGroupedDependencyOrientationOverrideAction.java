package seg.jUCMNav.actions;

import java.util.Iterator;

import org.eclipse.ui.IWorkbenchPart;

import grl.GroupedDependency;
import grl.GroupedDependencyLink;
import grl.LinkRef;
import seg.jUCMNav.Messages;
import seg.jUCMNav.editparts.GroupedDependencyEditPart;
import seg.jUCMNav.editparts.LinkRefEditPart;
import seg.jUCMNav.model.commands.transformations.ChangeGroupedDependencyOrientationOverrideCommand;
import seg.jUCMNav.strategies.util.ReusedElementUtil;

/**
 * Sets (or clears, with "None") the manual orientation override of a grouped dependency box, the "D"
 * glyph being forced toward a chosen side instead of the direction computed from the fan links.
 * 
 * <p>
 * Five such actions exist, one per value in
 * {@link ChangeGroupedDependencyOrientationOverrideCommand} (Up, Down, Left, Right, None). They are
 * collected in an "Orientation Override" submenu in the context menu.
 * </p>
 * 
 * <p>
 * The whole construct is the grouped dependency: each action is enabled on a single D box or on a
 * single fan link (a {@link LinkRef} whose definition is a {@link GroupedDependencyLink}).
 * </p>
 * 
 * @author skuly
 */
public class SetGroupedDependencyOrientationOverrideAction extends URNSelectionAction {

    public static final String SET_ORIENTATION_UP = "seg.jUCMNav.SetGroupedDependencyOrientationUp"; //$NON-NLS-1$
    public static final String SET_ORIENTATION_DOWN = "seg.jUCMNav.SetGroupedDependencyOrientationDown"; //$NON-NLS-1$
    public static final String SET_ORIENTATION_LEFT = "seg.jUCMNav.SetGroupedDependencyOrientationLeft"; //$NON-NLS-1$
    public static final String SET_ORIENTATION_RIGHT = "seg.jUCMNav.SetGroupedDependencyOrientationRight"; //$NON-NLS-1$
    public static final String CLEAR_ORIENTATION = "seg.jUCMNav.ClearGroupedDependencyOrientation"; //$NON-NLS-1$

    private final String value;
    private GroupedDependency box;

    /**
     * @param part
     * @param value
     *            the override side to apply, one of
     *            {@link ChangeGroupedDependencyOrientationOverrideCommand#RIGHT},
     *            {@link ChangeGroupedDependencyOrientationOverrideCommand#LEFT},
     *            {@link ChangeGroupedDependencyOrientationOverrideCommand#TOP},
     *            {@link ChangeGroupedDependencyOrientationOverrideCommand#BOTTOM} or
     *            {@link ChangeGroupedDependencyOrientationOverrideCommand#NONE}
     */
    public SetGroupedDependencyOrientationOverrideAction(IWorkbenchPart part, String value) {
        super(part);
        this.value = value;
        setId(idForValue(value));
        setText(Messages.getString("SetGroupedDependencyOrientationOverrideAction." + value)); //$NON-NLS-1$
    }

    private static String idForValue(String value) {
        if (ChangeGroupedDependencyOrientationOverrideCommand.TOP.equals(value))
            return SET_ORIENTATION_UP;
        if (ChangeGroupedDependencyOrientationOverrideCommand.BOTTOM.equals(value))
            return SET_ORIENTATION_DOWN;
        if (ChangeGroupedDependencyOrientationOverrideCommand.LEFT.equals(value))
            return SET_ORIENTATION_LEFT;
        if (ChangeGroupedDependencyOrientationOverrideCommand.RIGHT.equals(value))
            return SET_ORIENTATION_RIGHT;
        return CLEAR_ORIENTATION;
    }

    /**
     * We need exactly one grouped dependency box or one fan link selected.
     */
    protected boolean calculateEnabled() {
        box = null;
        int matches = 0;
        for (Iterator iter = getSelectedObjects().iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof GroupedDependencyEditPart) {
                box = (GroupedDependency) ((GroupedDependencyEditPart) obj).getModel();
            } else if (obj instanceof LinkRefEditPart) {
                LinkRef linkRef = ((LinkRefEditPart) obj).getLinkRef();
                if (ReusedElementUtil.isReuseLink(linkRef.getLink()))
                    return false;
                box = ChangeGroupedDependencyTargetMultiplicityAction.findGroupedDependency(linkRef);
                if (box == null)
                    return false;
            } else {
                return false;
            }
            matches++;
        }
        return matches == 1 && box != null;
    }

    public void run() {
        execute(new ChangeGroupedDependencyOrientationOverrideCommand(box, value));
    }
}