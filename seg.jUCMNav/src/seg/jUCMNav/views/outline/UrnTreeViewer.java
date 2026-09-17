package seg.jUCMNav.views.outline;

import org.eclipse.gef.ui.parts.TreeViewer;

public class UrnTreeViewer extends TreeViewer {

    public UrnTreeViewer() {

    }

    // GEF 3.25's AbstractEditPartViewer never clears its dropTarget field when the control is
    // disposed: SWT disposes the DropTarget (nulling its transferAgents) but the field keeps
    // referencing it, and re-creating the control reuses the stale target - setTransfer() then
    // NPEs in DropTarget. Drop it here, while the old control is being un-hooked, so that the
    // next hookControl() builds a fresh DropTarget.
    @Override
    protected void unhookControl() {
        setDropTarget(null);
        super.unhookControl();
    }

    // bug 531: causes mucho memory leaks

    /*
     * protected UCMNavMultiPageEditor editor; public UrnTreeViewer(UCMNavMultiPageEditor editor) { this.editor=editor; } public UCMNavMultiPageEditor
     * getEditor() { return editor; } public void setEditor(UCMNavMultiPageEditor editor) { this.editor = editor; }
     */
}
