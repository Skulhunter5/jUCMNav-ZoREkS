package seg.jUCMNav.model.commands.transformations;

import org.eclipse.gef.commands.Command;

import grl.GroupedDependency;
import seg.jUCMNav.Messages;
import seg.jUCMNav.model.commands.JUCMNavCommand;
import seg.jUCMNav.model.util.MetadataHelper;
import urn.URNspec;

/**
 * Stores (or clears, with {@link #NONE}) a manual target-side override on a grouped dependency box,
 * in addition to the automatic orientation computed from the fan links.
 * 
 * <p>
 * The override lives as element metadata on the {@link GroupedDependency} itself
 * (key {@link #ORIENTATION_OVERRIDE_KEY}), so it survives serialization and the automatic
 * re-orientation in the edit part defers to it while it is set.
 * </p>
 * 
 * @author skuly
 */
public class ChangeGroupedDependencyOrientationOverrideCommand extends Command implements JUCMNavCommand {

    public static final String ORIENTATION_OVERRIDE_KEY = "seg.jUCMNav.groupedDependency.orientationOverride"; //$NON-NLS-1$

    public static final String NONE = "NONE"; //$NON-NLS-1$
    public static final String RIGHT = "RIGHT"; //$NON-NLS-1$
    public static final String LEFT = "LEFT"; //$NON-NLS-1$
    public static final String TOP = "TOP"; //$NON-NLS-1$
    public static final String BOTTOM = "BOTTOM"; //$NON-NLS-1$

    private final GroupedDependency groupedDependency;
    private final String newValue;
    private String oldValue;

    /**
     * @param groupedDependency
     *            the grouped dependency whose orientation override changes
     * @param newValue
     *            the override side ({@link #RIGHT}, {@link #LEFT}, {@link #TOP} or {@link #BOTTOM});
     *            {@link #NONE} or {@code null} clears the override
     */
    public ChangeGroupedDependencyOrientationOverrideCommand(GroupedDependency groupedDependency, String newValue) {
        this.groupedDependency = groupedDependency;
        this.newValue = newValue == null ? NONE : newValue;
        setLabel(Messages.getString("ChangeGroupedDependencyOrientationOverrideCommand.label")); //$NON-NLS-1$
    }

    /**
     * @see org.eclipse.gef.commands.Command#canExecute()
     */
    public boolean canExecute() {
        return groupedDependency != null;
    }

    private URNspec getUrnspec() {
        return groupedDependency.getGrlspec().getUrnspec();
    }

    private void apply(String value) {
        if (value == null || NONE.equals(value)) {
            MetadataHelper.removeMetaData(groupedDependency, ORIENTATION_OVERRIDE_KEY);
        } else {
            MetadataHelper.addMetaData(getUrnspec(), groupedDependency, ORIENTATION_OVERRIDE_KEY, value);
        }
    }

    /**
     * @see org.eclipse.gef.commands.Command#execute()
     */
    public void execute() {
        oldValue = MetadataHelper.getMetaData(groupedDependency, ORIENTATION_OVERRIDE_KEY);
        redo();
    }

    /**
     * @see org.eclipse.gef.commands.Command#redo()
     */
    public void redo() {
        testPreConditions();
        apply(newValue);
        testPostConditions();
    }

    /**
     * @see org.eclipse.gef.commands.Command#undo()
     */
    public void undo() {
        testPostConditions();
        apply(oldValue);
        testPreConditions();
    }

    /**
     * @see seg.jUCMNav.model.commands.JUCMNavCommand#testPreConditions()
     */
    public void testPreConditions() {
        assert groupedDependency != null : "pre grouped dependency missing"; //$NON-NLS-1$
    }

    /**
     * @see seg.jUCMNav.model.commands.JUCMNavCommand#testPostConditions()
     */
    public void testPostConditions() {
        assert groupedDependency != null : "post grouped dependency missing"; //$NON-NLS-1$
    }
}