package seg.jUCMNav.views.customCommonNavigator.actions;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ResourceTransfer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Deterministic regression tests for the trusted-clipboard marker gate around
 * {@link ResourceTransfer} in the Custom Common Navigator's copy/paste actions.
 * <p>
 * The platform's navigator {@code PasteAction} reads the clipboard through
 * {@link ResourceTransfer} on every selection change. {@link ResourceTransfer}
 * decodes the first 4 bytes of whatever the clipboard owner serves for its
 * target as a resource count, so a clipboard owned by an unrelated application
 * can throw a {@link NegativeArraySizeException} or make the platform log
 * "Transfer aborted, too many resources" (Eclipse bug 205678). The safe clones
 * only read the resource payload when the clipboard carries the
 * {@link JUCMResourceCopyMarkerTransfer} marker written by
 * {@link SafeCopyAction}.
 * </p>
 * <p>
 * The malformed payload is synthesized here by registering a
 * {@link ByteArrayTransfer} under {@link ResourceTransfer}'s type name and
 * writing garbage bytes to the real native clipboard through it, reproducing
 * end to end through SWT exactly the data shape that makes the unguarded
 * platform action crash. The type name is a random per-JVM string (see
 * {@code ResourceTransfer.TYPE_NAME}), so it is obtained reflectively.
 * </p>
 *
 * @author skuly
 */
public class SafeEditActionsMarkerGateTest {

    private Display display;
    private Shell shell;
    private Clipboard clipboard;
    private IProject project;
    private IFile file;
    private SafePasteAction pasteAction;
    private SafeCopyAction copyAction;

    /**
     * A native clipboard owner that advertises the Eclipse resource target but
     * serves unparsable bytes for it, like the misbehaving clipboard agents
     * that trigger bug 205678.
     */
    private final class GarbageResourceBytesTransfer extends ByteArrayTransfer {

        private final int typeId;
        private final String typeName;

        GarbageResourceBytesTransfer(String name) {
            typeName = name;
            typeId = registerType(name);
        }

        @Override
        protected int[] getTypeIds() {
            return new int[] { typeId };
        }

        @Override
        protected String[] getTypeNames() {
            return new String[] { typeName };
        }

        @Override
        protected void javaToNative(Object data, TransferData transferData) {
            super.javaToNative((byte[]) data, transferData);
        }

        @Override
        protected Object nativeToJava(TransferData transferData) {
            return super.nativeToJava(transferData);
        }
    }

    /**
     * Four 0xFF bytes decode to a negative resource count, which is what makes
     * {@link ResourceTransfer#nativeToJava(TransferData)} throw
     * {@link NegativeArraySizeException} - a negative count slips past the
     * platform's own "too many resources" guard (bug 205678).
     */
    private static final byte[] GARBAGE_RESOURCE_PAYLOAD = new byte[] {
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

    @Before
    public void setUp() throws Exception {
        display = PlatformUI.getWorkbench().getDisplay();
        shell = new Shell(display);
        clipboard = new Clipboard(display);

        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        project = workspaceRoot.getProject("jUCMNav-copy-paste-marker-test"); //$NON-NLS-1$
        if (project.exists()) {
            project.delete(true, true, null);
        }
        project.create(null);
        project.open(null);
        file = project.getFile("marker-test.txt"); //$NON-NLS-1$
        file.create(new ByteArrayInputStream("hello".getBytes()), false, null); //$NON-NLS-1$

        pasteAction = new SafePasteAction(shell, clipboard);
        copyAction = new SafeCopyAction(shell, clipboard, pasteAction);
    }

    @After
    public void tearDown() throws Exception {
        if (clipboard != null) {
            clipboard.dispose();
            clipboard = null;
        }
        if (shell != null) {
            shell.dispose();
            shell = null;
        }
        if (project != null && project.exists()) {
            project.delete(true, true, null);
            project = null;
        }
    }

    /**
     * @return the per-JVM random type name under which
     *         {@link ResourceTransfer} registers its native format (field has
     *         been named {@code TYPE_NAME} for two decades).
     */
    private String resourceTypeName() throws Exception {
        ResourceTransfer.getInstance();
        Field typeNameField = ResourceTransfer.class.getDeclaredField("TYPE_NAME"); //$NON-NLS-1$
        typeNameField.setAccessible(true);
        return (String) typeNameField.get(null);
    }

    /**
     * A real jUCMNav copy stamps the marker, keeps the platform resource
     * payload readable, and the paste action stays enabled.
     */
    @Test
    public void testSafeCopyStampsMarkerAndResourcePasteStaysEnabled() {
        copyAction.selectionChanged(new StructuredSelection(file));
        copyAction.run();

        Object marker = clipboard.getContents(JUCMResourceCopyMarkerTransfer.INSTANCE);
        assertNotNull("SafeCopyAction must stamp the trusted-copy marker", marker);
        assertTrue("marker content must round-trip", ((Boolean) marker).booleanValue());

        IResource[] resources = (IResource[]) clipboard.getContents(ResourceTransfer.getInstance());
        assertNotNull("the platform resource payload must still be readable", resources);
        assertTrue("the copied resource must survive the round-trip",
                file.getFullPath().equals(resources[0].getFullPath()));

        pasteAction.selectionChanged(new StructuredSelection(project));
        assertTrue("paste of a trusted resource copy must be enabled", pasteAction.isEnabled());
    }

    /**
     * A foreign clipboard that serves garbage for the resource target must
     * never reach {@link ResourceTransfer} when no marker is present. The
     * unguarded platform action throws {@link NegativeArraySizeException} on
     * this exact data.
     */
    @Test
    public void testForeignGarbageClipboardWithoutMarkerIsIgnored() throws Exception {
        GarbageResourceBytesTransfer garbage = new GarbageResourceBytesTransfer(resourceTypeName());
        clipboard.setContents(new Object[] { GARBAGE_RESOURCE_PAYLOAD }, new Transfer[] { garbage });

        byte[] roundTrip = (byte[]) clipboard.getContents(garbage);
        assertNotNull("garbage must be reachable under the resource format", roundTrip);

        try {
            pasteAction.selectionChanged(new StructuredSelection(project));
        } catch (Exception | LinkageError e) {
            fail("untrusted clipboard data must not propagate: " + e);
        }
        assertTrue("paste of an untrusted clipboard must stay disabled", !pasteAction.isEnabled());
    }

    /**
     * Even with the marker present, a payload whose first word is a negative
     * count is swallowed by the guard instead of crashing updateSelection.
     */
    @Test
    public void testGarbageResourcePayloadWithMarkerIsSwallowed() throws Exception {
        GarbageResourceBytesTransfer garbage = new GarbageResourceBytesTransfer(resourceTypeName());
        clipboard.setContents(
                new Object[] { Boolean.TRUE, GARBAGE_RESOURCE_PAYLOAD },
                new Transfer[] { JUCMResourceCopyMarkerTransfer.INSTANCE, garbage });

        try {
            pasteAction.selectionChanged(new StructuredSelection(project));
        } catch (Exception | LinkageError e) {
            fail("malformed resource payload must be swallowed, not thrown: " + e);
        }
        assertTrue("paste of a malformed resource copy must stay disabled", !pasteAction.isEnabled());
    }

    /**
     * The user-reported scenario: the clipboard simply holds text from another
     * application. Paste must neither throw nor enable.
     */
    @Test
    public void testPlainTextClipboardIsIgnored() {
        clipboard.setContents(new Object[] { "some copied text" }, new Transfer[] { TextTransfer.getInstance() });

        pasteAction.selectionChanged(new StructuredSelection(project));
        assertTrue("paste of plain text must stay disabled", !pasteAction.isEnabled());
    }

    /**
     * A clipboard written by an external file manager (file names, no marker)
     * must still enable the paste action exactly as before.
     */
    @Test
    public void testExternalFileManagerClipboardStillPastes() {
        clipboard.setContents(
                new Object[] { new String[] { project.getLocation().toOSString() } },
                new Transfer[] { FileTransfer.getInstance() });

        pasteAction.selectionChanged(new StructuredSelection(project));
        assertTrue("file-manager content must remain pasteable", pasteAction.isEnabled());
    }
}