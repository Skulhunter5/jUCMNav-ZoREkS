/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package seg.jUCMNav.views.customCommonNavigator.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

/**
 * The platform's internal navigator EditActionProvider, replaced by this
 * override so that the Custom Common Navigator never reads the clipboard
 * through {@link org.eclipse.ui.part.ResourceTransfer} on behalf of an
 * untrusted clipboard owner. Registered (and thereby suppressing the platform
 * provider) via the <code>overrides</code> attribute of the
 * &lt;actionProvider/&gt; element in plugin.xml.
 *
 * @author skuly
 */
public class SafeEditActionProvider extends CommonActionProvider {

	private SafeEditActionGroup editGroup;

	@Override
	public void init(ICommonActionExtensionSite anActionSite) {
		editGroup = new SafeEditActionGroup(anActionSite.getViewSite().getShell());
	}

	@Override
	public void dispose() {
		if (editGroup != null) {
			editGroup.dispose();
		}
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		editGroup.fillActionBars(actionBars);
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		editGroup.fillContextMenu(menu);
	}

	@Override
	public void setContext(ActionContext context) {
		editGroup.setContext(context);
	}

	@Override
	public void updateActionBars() {
		editGroup.updateActionBars();
	}
}