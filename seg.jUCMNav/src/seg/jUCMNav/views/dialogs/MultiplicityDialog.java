package seg.jUCMNav.views.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import seg.jUCMNav.Messages;
import seg.jUCMNav.model.util.DependencyMultiplicity;

/**
 * Asks for the [x..y] multiplicity of a dependency end. An empty value clears the end's
 * multiplicity.
 * 
 * @author skuly
 */
public class MultiplicityDialog extends Dialog {

    private Text text;
    private String result;
    private String initialValue;

    public MultiplicityDialog(Shell parentShell, String initialValue) {
        super(parentShell);
        this.result = "";
        this.initialValue = initialValue == null ? "" : initialValue;
    }

    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.getString("MultiplicityDialog.title")); //$NON-NLS-1$
    }

    /**
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 12;
        layout.marginHeight = 12;
        layout.verticalSpacing = 8;
        composite.setLayout(layout);

        Label label = new Label(composite, SWT.NONE);
        label.setText(Messages.getString("MultiplicityDialog.multiplicity")); //$NON-NLS-1$

        text = new Text(composite, SWT.BORDER | SWT.SINGLE);
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        text.setText(initialValue);

        return composite;
    }

    /**
     * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
     */
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getShell().setDefaultButton(getButton(IDialogConstants.OK_ID));
    }

    /**
     * Validates and captures the value before the widgets are disposed; on invalid input the
     * dialog stays open instead of closing.
     * 
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    protected void okPressed() {
        String input = text.getText().trim();
        if (DependencyMultiplicity.isValid(input)) {
            result = input.length() == 0 ? "" : DependencyMultiplicity.normalizeStored(input);
            super.okPressed();
        } else {
            MessageDialog.openError(getShell(), Messages.getString("MultiplicityDialog.title"), //$NON-NLS-1$
                    Messages.getString("MultiplicityDialog.invalid")); //$NON-NLS-1$
        }
    }

    /**
     * @return the multiplicity to store on the dependency end (empty string to clear it); only
     *         meaningful after OK was pressed
     */
    public String getValue() {
        return result;
    }
}