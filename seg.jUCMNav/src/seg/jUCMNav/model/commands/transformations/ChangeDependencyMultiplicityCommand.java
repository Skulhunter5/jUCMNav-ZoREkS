package seg.jUCMNav.model.commands.transformations;

import org.eclipse.gef.commands.Command;

import grl.Dependency;
import seg.jUCMNav.Messages;
import seg.jUCMNav.model.commands.JUCMNavCommand;
import seg.jUCMNav.model.util.DependencyMultiplicity;

/**
 * Sets (or clears, with an empty value) the multiplicity of one end of a GRL dependency.
 * 
 * <p>
 * Multiplicities live on the dependency definition ({@code srcMultiplicity} /
 * {@code destMultiplicity}), so this command naturally supports undo/redo and is shared by every
 * link reference and editor displaying that dependency.
 * </p>
 * 
 * @author skuly
 */
public class ChangeDependencyMultiplicityCommand extends Command implements JUCMNavCommand {

    /** The source end of the dependency. */
    public static final int SOURCE = 0;
    /** The target end of the dependency. */
    public static final int TARGET = 1;

    private Dependency dependency;
    private int end;
    private String oldValue;
    private String newValue;

    /**
     * @param dependency
     *            the dependency whose multiplicity changes
     * @param end
     *            {@link #SOURCE} or {@link #TARGET}
     * @param newValue
     *            the new multiplicity ({@code x..y}, {@code *} allowed; empty clears it); it is
     *            normalized, not validated here
     */
    public ChangeDependencyMultiplicityCommand(Dependency dependency, int end, String newValue) {
        this.dependency = dependency;
        this.end = end;
        this.newValue = DependencyMultiplicity.normalizeStored(newValue);
        setLabel(Messages.getString("ChangeDependencyMultiplicityCommand.label")); //$NON-NLS-1$
    }

    /**
     * @see org.eclipse.gef.commands.Command#canExecute()
     */
    public boolean canExecute() {
        return dependency != null && (end == SOURCE || end == TARGET);
    }

    /**
     * @see org.eclipse.gef.commands.Command#execute()
     */
    public void execute() {
        oldValue = currentValue();
        redo();
    }

    /**
     * @see org.eclipse.gef.commands.Command#redo()
     */
    public void redo() {
        testPreConditions();
        set(dependency, end, newValue);
        testPostConditions();
    }

    /**
     * @see org.eclipse.gef.commands.Command#undo()
     */
    public void undo() {
        testPostConditions();
        set(dependency, end, oldValue);
        testPreConditions();
    }

    private String currentValue() {
        return (end == SOURCE) ? dependency.getSrcMultiplicity() : dependency.getDestMultiplicity();
    }

    private static void set(Dependency dependency, int end, String value) {
        if (end == SOURCE)
            dependency.setSrcMultiplicity(value);
        else
            dependency.setDestMultiplicity(value);
    }

    /**
     * @see seg.jUCMNav.model.commands.JUCMNavCommand#testPreConditions()
     */
    public void testPreConditions() {
        assert dependency != null : "pre dependency missing"; //$NON-NLS-1$
    }

    /**
     * @see seg.jUCMNav.model.commands.JUCMNavCommand#testPostConditions()
     */
    public void testPostConditions() {
        assert dependency != null : "post dependency missing"; //$NON-NLS-1$
        assert currentValue().equals(newValue) : "post multiplicity not applied"; //$NON-NLS-1$
    }
}