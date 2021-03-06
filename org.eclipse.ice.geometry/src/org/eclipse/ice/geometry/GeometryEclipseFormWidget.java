/*******************************************************************************
 * Copyright (c) 2015 UT-Battelle, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Initial API and implementation and/or initial documentation - Jay Jay Billings,
 *   Jordan H. Deyton, Dasha Gorin, Alexander J. McCaskey, Taylor Patterson,
 *   Claire Saunders, Matthew Wang, Anna Wojtowicz, Kasper Gammeltoft, Robert Smith
 *******************************************************************************/
package org.eclipse.ice.geometry;

import org.eclipse.ice.client.widgets.EclipseFormWidget;
import org.eclipse.ice.client.widgets.ICEFormEditor;
import org.eclipse.ice.client.widgets.ICEFormInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * A widget which will display a GeometryFormEditor.
 * 
 * @author Kasper Gammeltoft, Jordan H. Deyton, Robert Smith
 *
 */
public class GeometryEclipseFormWidget extends EclipseFormWidget {

	/**
	 * The default constructor.
	 */
	public GeometryEclipseFormWidget() {
	}

	/**
	 * This operation displays the {@link ReflectivityFormEditor} instead of the
	 * standard ICEFormEditor.
	 */
	@Override
	public void display() {

		// Local Declarations
		IWorkbenchPage page = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();

		// Create the ICEFormInput for the ReflectivityFormBuilder.
		ICEFormInput = new ICEFormInput(widgetForm);

		try {
			// Use the workbench to open the editor with our input.
			IEditorPart formEditor = page.openEditor(ICEFormInput,
					GeometryFormEditor.ID);
			// Set this editor reference so that listeners can be registered
			// later.
			ICEFormEditor = (ICEFormEditor) formEditor;

		} catch (PartInitException e) {
			// Dump the stacktrace if something happens.
			e.printStackTrace();
		}

		return;
	}

}
