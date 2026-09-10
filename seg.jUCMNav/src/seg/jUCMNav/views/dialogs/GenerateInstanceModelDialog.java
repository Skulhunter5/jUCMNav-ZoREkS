/**
 * 
 */
package seg.jUCMNav.views.dialogs;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import grl.ActorRef;
import grl.GRLGraph;
import seg.jUCMNav.Messages;
import seg.jUCMNav.model.commands.create.GenerateInstanceModelCommand;
import seg.jUCMNav.model.util.URNNamingHelper;

/**
 * Dialog asking how many instances of each actor (and of the margin between actor cells) must
 * be generated when creating an instance model from a GRL type-model graph.
 * 
 * @author skuly
 */
public class GenerateInstanceModelDialog extends Dialog {

    private GRLGraph graph;
    private Spinner marginSpinner;
    private LinkedHashMap<ActorRef, Spinner> spinners = new LinkedHashMap<ActorRef, Spinner>();

    private int selectedMargin;
    private Map<ActorRef, Integer> selectedCounts;

    /**
     * @param parentShell
     * @param graph
     *            the type-model graph being instantiated; its actor refs become the rows of the
     *            dialog, in graph order
     */
    public GenerateInstanceModelDialog(Shell parentShell, GRLGraph graph) {
        super(parentShell);
        this.graph = graph;
    }

    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.getString("GenerateInstanceModelDialog.title")); //$NON-NLS-1$
    }

    /**
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 12;
        layout.marginHeight = 12;
        layout.verticalSpacing = 8;
        composite.setLayout(layout);

        GridData labelData = new GridData(SWT.END, SWT.CENTER, false, false);
        GridData spinnerData = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);

        for (ActorRef actorRef : (java.util.List<ActorRef>) graph.getContRefs()) {
            Label label = new Label(composite, SWT.NONE);
            label.setText(URNNamingHelper.getName(actorRef));
            label.setLayoutData(labelData);

            Spinner spinner = new Spinner(composite, SWT.BORDER);
            spinner.setMinimum(1);
            spinner.setMaximum(100);
            spinner.setIncrement(1);
            spinner.setPageIncrement(1);
            spinner.setSelection(1);
            spinner.setLayoutData(spinnerData);
            spinners.put(actorRef, spinner);
        }

        Label marginLabel = new Label(composite, SWT.NONE);
        marginLabel.setText(Messages.getString("GenerateInstanceModelDialog.margin")); //$NON-NLS-1$
        marginLabel.setLayoutData(labelData);

        marginSpinner = new Spinner(composite, SWT.BORDER);
        marginSpinner.setMinimum(0);
        marginSpinner.setMaximum(500);
        marginSpinner.setIncrement(5);
        marginSpinner.setPageIncrement(5);
        marginSpinner.setSelection(getDefaultMargin());
        marginSpinner.setLayoutData(spinnerData);

        return composite;
    }

    /**
     * The dialog default for the margin, so it can be adjusted in one place in the code.
     */
    private int getDefaultMargin() {
        return GenerateInstanceModelCommand.DEFAULT_MARGIN;
    }

    /**
     * Reads the spinner values before the widgets are disposed: the caller only gets them via
     * {@link #getCounts()} after {@link Dialog#open()} returned, at which point the SWT widgets no
     * longer exist.
     * 
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    protected void okPressed() {
        selectedMargin = marginSpinner.getSelection();
        selectedCounts = new LinkedHashMap<ActorRef, Integer>();
        for (Map.Entry<ActorRef, Spinner> entry : spinners.entrySet())
            selectedCounts.put(entry.getKey(), Integer.valueOf(entry.getValue().getSelection()));
        super.okPressed();
    }

    /**
     * @return the number of copies asked for each actor, in dialog (graph) order; only meaningful
     *         after OK was pressed
     */
    public Map<ActorRef, Integer> getCounts() {
        return selectedCounts;
    }

    /**
     * @return the spacing (in pixels) between actor rows and between actor cells; only meaningful
     *         after OK was pressed
     */
    public int getMargin() {
        return selectedCounts != null ? selectedMargin : getDefaultMargin();
    }
}