package seg.jUCMNav.model.commands.transformations;

import org.eclipse.gef.commands.Command;

import grl.GroupedDependency;
import seg.jUCMNav.Messages;
import seg.jUCMNav.model.commands.JUCMNavCommand;
import seg.jUCMNav.model.util.DependencyMultiplicity;

/**
 * Sets (or clears, with an empty value) the target multiplicity displayed by a grouped dependency.
 * 
 * <p>
 * For grouped dependencies the target multiplicity is stored on the grouped dependency box itself
 * ({@link GroupedDependency#getDestMultiplicity()}), not on the shared dependency definition, because
 * the box is the only place displaying it.
 * </p>
 * 
 * @author skuly
 */
public class ChangeGroupedDependencyTargetMultiplicityCommand extends Command implements JUCMNavCommand {

    private GroupedDependency groupedDependency;
    private String oldValue;
    private String newValue;

    /**
     * @param groupedDependency
     *            the grouped dependency whose target multiplicity changes
     * @param newValue
     *            the new multiplicity ({@code x..y}, {@code *} allowed; empty clears it); it is
     *            normalized, not validated here
     */
    public ChangeGroupedDependencyTargetMultiplicityCommand(GroupedDependency groupedDependency, String newValue) {
        this.groupedDependency = groupedDependency;
        this.newValue = DependencyMultiplicity.normalizeStored(newValue);
        setLabel(Messages.getString("ChangeGroupedDependencyTargetMultiplicityCommand.label")); //$NON-NLS-1$
    }

    /**
     * @see org.eclipse.gef.commands.Command#canExecute()
     */
    public boolean canExecute() {
        return groupedDependency != null;
    }

    /**
     * @see org.eclipse.gef.commands.Command#execute()
     */
    public void execute() {
        oldValue = groupedDependency.getDestMultiplicity();
        redo();
    }

    /**
     * @see org.eclipse.gef.commands.Command#redo()
     */
    public void redo() {
        testPreConditions();
        groupedDependency.setDestMultiplicity(newValue);
        testPostConditions();
    }

    /**
     * @see org.eclipse.gef.commands.Command#undo()
     */
    public void undo() {
        testPostConditions();
        groupedDependency.setDestMultiplicity(oldValue);
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
        assert groupedDependency.getDestMultiplicity().equals(newValue) : "post multiplicity not applied"; //$NON-NLS-1$
    }
}