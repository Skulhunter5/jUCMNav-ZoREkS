/*******************************************************************************
 * Copyright (c) 2026 University of Ottawa and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package seg.jUCMNav.views.customCommonNavigator.actions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

/**
 * A purely internal clipboard transfer that marks clipboards written by
 * {@link SafeCopyAction}. {@link SafePasteAction} refuses to interpret the
 * clipboard through {@link org.eclipse.ui.part.ResourceTransfer} unless this
 * marker is present.
 * <p>
 * This gate exists because {@link ResourceTransfer} decodes the first 4 bytes
 * of whatever an arbitrary clipboard owner serves for the <i>resource</i>
 * target as a resource count. A clipboard owned by an unrelated application
 * (plain text, an image, an external file manager) can therefore make the
 * platform log "Transfer aborted, too many resources" or throw a
 * {@link NegativeArraySizeException} from inside
 * {@code ResourceTransfer.nativeToJava} (see Eclipse bug 205678).
 * </p>
 *
 * @author skuly
 */
public final class JUCMResourceCopyMarkerTransfer extends ByteArrayTransfer {

	/**
	 * The singleton instance.
	 */
	public static final JUCMResourceCopyMarkerTransfer INSTANCE = new JUCMResourceCopyMarkerTransfer();

	/**
	 * Fixed magic int prepended by {@link #javaToNative(Object, TransferData)}.
	 * The atom name is deliberately a fixed, unguessable-per-project constant
	 * rather than a timestamped one: only {@link SafeCopyAction} writes it and
	 * only {@link SafePasteAction} reads it, and both live in this plug-in.
	 */
	private static final int MAGIC = 0x4A55434E;

	/**
	 * The atom name registered with the platform.
	 */
	private static final String TYPE_NAME = "urn:seg.jUCMNav:resource-copy-marker"; //$NON-NLS-1$

	private static final int TYPE_ID = registerType(TYPE_NAME);

	private JUCMResourceCopyMarkerTransfer() {
	}

	@Override
	protected int[] getTypeIds() {
		return new int[] { TYPE_ID };
	}

	@Override
	protected String[] getTypeNames() {
		return new String[] { TYPE_NAME };
	}

	@Override
	protected void javaToNative(Object data, TransferData transferData) {
		if (!(data instanceof Boolean)) {
			return;
		}
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(out);
			dataOut.writeInt(MAGIC);
			dataOut.flush();
			super.javaToNative(out.toByteArray(), transferData);
		} catch (IOException e) {
			// the marker could not be marshalled; the clipboard simply stays untrusted
		}
	}

	@Override
	protected Object nativeToJava(TransferData transferData) {
		Object data = super.nativeToJava(transferData);
		if (!(data instanceof byte[])) {
			return null;
		}
		byte[] bytes = (byte[]) data;
		if (bytes.length < Integer.SIZE / Byte.SIZE) {
			return null;
		}
		try {
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
			return in.readInt() == MAGIC ? Boolean.TRUE : null;
		} catch (IOException e) {
			return null;
		}
	}
}