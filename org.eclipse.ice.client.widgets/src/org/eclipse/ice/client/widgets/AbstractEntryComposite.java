package org.eclipse.ice.client.widgets;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ice.datastructures.ICEObject.IUpdateable;
import org.eclipse.ice.datastructures.entry.IEntry;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IMessageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractEntryComposite extends Composite implements IEntryComposite { 

	/**
	 * Logger for handling event messages and other information.
	 */
	protected static final Logger logger = LoggerFactory.getLogger(AbstractEntryComposite.class);

	/**
	 * A label that describes the Entry.
	 */
	protected Label label;

	/**
	 * The Entry that is displayed by the EntryComposite.
	 */
	protected IEntry entry;

	/**
	 * Reference to the widget this Composite will contain
	 */
	protected Control widget;

	/**
	 * The currently set value of the Entry.
	 */
	protected String currentSelection;

	/**
	 * Entry map of binary/boolean-type allowed values
	 */
	protected final List<String> allowedBinaryValues = new ArrayList<String>();

	/**
	 * List of allowed values for the entry in lowercase text
	 */
	protected final List<String> lowercaseAllowedValues = new ArrayList<String>();

	/**
	 * A set of buttons for the Entry.
	 */
	protected final List<Button> buttons;

	/**
	 * The message manager to which message about the success or failure of
	 * manipulating the Entry should be posted.
	 */
	protected IMessageManager messageManager;

	/**
	 * The name of the message posted to the message manager.
	 */
	protected String messageName;

	/**
	 * This listens to the {@code EntryComposite}'s resize events and adjusts
	 * the size of the dropdown if necessary. This is currently only used for
	 * file entries.
	 */
	protected ControlListener resizeListener = null;

	/**
	 * A ControlDecoration that can be added to the EntryComposite if desired.
	 */
	protected ControlDecoration decoration = null;

	/**
	 * The Constructor
	 *
	 * @param parent
	 *            The parent Composite.
	 * @param style
	 *            The style of the EntryComposite.
	 * @param refEntry
	 *            An Entry that should be used to create the widget, to update
	 *            when changed by the user and to be updated from when changed
	 *            internally by ICE.
	 */
	public AbstractEntryComposite(Composite parent, IEntry refEntry, int style) {

		// Call the super constructor
		super(parent, style);

		// Set the Entry reference
		if (refEntry != null) {
			entry = refEntry;
		} else {
			throw new RuntimeException("Entry passed to EntryComposite "
					+ "constructor cannot be null!");
		}

		// Setup the allowedBinaryValues for check boxes
		// Setup the list of values that are equivalent to "ready"
		allowedBinaryValues.add("ready");
		allowedBinaryValues.add("yes");
		allowedBinaryValues.add("y");
		allowedBinaryValues.add("true");
		allowedBinaryValues.add("enabled");
		allowedBinaryValues.add("on");
		// Setup the list of values that are equivalent to "not ready"
		allowedBinaryValues.add("not ready");
		allowedBinaryValues.add("no");
		allowedBinaryValues.add("n");
		allowedBinaryValues.add("false");
		allowedBinaryValues.add("disabled");
		allowedBinaryValues.add("off");

		// Create the Buttons array
		buttons = new ArrayList<Button>();

		// Create the MessageName String
		messageName = new String();
		messageName = entry.getName() + " " + entry.getId();

		// Register for updates from the Entry
		entry.register(this);

		// Add a listener to the Entry that unregisters this composite as a
		// listener upon disposal.
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				entry.unregister(AbstractEntryComposite.this);
			}
		});

		// Get a reference to the current Entry value
		currentSelection = entry.getValue();

		// Render the entry
		render();

		return;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ice.client.widgets.IEntryComposite#getComposite()
	 */
	public Composite getComposite() {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ice.client.widgets.IEntryComposite#render()
	 */
	public abstract void render();

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ice.client.widgets.IEntryComposite#refresh()
	 */
	@Override
	public void refresh() {

		logger.info("Refresh called for " + entry.getName());
		if (label != null) {
			label.dispose();
			label = null;
		}
		
		if (widget != null) {
			logger.info("Disposing widget - " + widget.getClass().getCanonicalName());
			widget.dispose();
			widget = null;
		}
		
		for (Button button : buttons) {
			if (!button.isDisposed()) {
				button.dispose();
			}
		}

		// Remove all of the previous buttons.
		buttons.clear();

		// Print an error if this Entry has been prematurely disposed.
		if (isDisposed()) {
			logger.info("EntryComposite Message: "
					+ "This composite has been prematurely disposed!");
			return;
		}

		// Remove the resize listener.
		if (resizeListener != null) {
			removeControlListener(resizeListener);
			resizeListener = null;
		}

		// Re-render the Composite
		render();

		// Re-draw the Composite
		layout();

		return;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ice.client.widgets.IEntryComposite#setMessageManager(org.eclipse.ui.forms.IMessageManager)
	 */
	public void setMessageManager(IMessageManager manager) {

		// Set the messageManager
		messageManager = manager;

		return;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ice.client.widgets.IEntryComposite#getEntry()
	 */
	public IEntry getEntry() {
		return entry;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ice.client.widgets.IEntryComposite#setEntry(org.eclipse.ice.datastructures.entry.IEntry)
	 */
	public void setEntry(IEntry ent) {
		entry = ent;
		refresh();
	}
	
	/**
	 * Creates a label for the EntryComposite.
	 */
	protected void createLabel() {

		// Create the Label
		label = new Label(this, SWT.WRAP);
		label.setText(entry.getName() + ":");
		label.setToolTipText(entry.getDescription());
		label.setBackground(getBackground());
		return;
	}

	/**
	 * This method is responsible for toggling a ControlDecoration on and off on
	 * the EntryComposite. The decoration will toggle on if the editor is dirty
	 * and the selection was recently changed (monitored by
	 * {@link EntryComposite#currentSelection}). Otherwise, it will toggle off.
	 */
	public void toggleSaveDecoration() {

		if (decoration == null) {
			// Create a new decoration and message
			decoration = new ControlDecoration(this, SWT.TOP | SWT.LEFT);
			final String saveMessage = "The form contains unsaved changes";

			// Set a description and image
			decoration.setDescriptionText(saveMessage);
			Image image = FieldDecorationRegistry.getDefault()
					.getFieldDecoration(FieldDecorationRegistry.DEC_WARNING)
					.getImage();
			decoration.setImage(image);

			// Set a listener to hide/show the decoration according to the
			// editor's state and the current entry value
			final IEditorPart editor = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage()
					.getActiveEditor();
			editor.addPropertyListener(new IPropertyListener() {
				@Override
				public void propertyChanged(Object source, int propId) {
					// Toggle the decoration on if the form is dirty and the
					// value has changed
					if (editor != null) {
						if (editor.isDirty() && !AbstractEntryComposite.this.entry
								.getValue().equals(currentSelection)) {
							// Show the decoration
							AbstractEntryComposite.this.decoration.show();
						} else if (!editor.isDirty()) {
							// Hide the decoration
							AbstractEntryComposite.this.decoration.hide();
						}
					}

					return;
				}
			});
		}

		// If the decoration already exists, check the Entry's state and set the
		// decoration as needed.
		else {
			final IEditorPart editor = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage()
					.getActiveEditor();
			if (editor != null) {
				if (editor.isDirty() && !AbstractEntryComposite.this.entry.getValue()
						.equals(currentSelection)) {
					// Show the decoration
					AbstractEntryComposite.this.decoration.show();
				} else if (!editor.isDirty()) {
					// Hide the decoration
					AbstractEntryComposite.this.decoration.hide();
				}
			}
		}

		return;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ice.datastructures.ICEObject.IUpdateableListener#update(org.eclipse.ice.datastructures.ICEObject.IUpdateable)
	 */
	@Override
	public void update(IUpdateable component) {
		// When the Entry has updated, refresh on the Eclipse UI thread.
		if (component == entry) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!AbstractEntryComposite.this.isDisposed()) {
						logger.info("Updating " + entry.getName() + " Entry Composite.");
					
						// Refresh the EntryComposite
						refresh();

						// Toggle the "unsaved changes" decoration if the entry
						// value is different
						if (!AbstractEntryComposite.this.entry.getValue().equals(currentSelection)) {
							toggleSaveDecoration();
						}

						// Update the reference to the Entry's value
						currentSelection = AbstractEntryComposite.this.entry.getValue();

					} else {
						entry.unregister(AbstractEntryComposite.this);
					}
				}
			});
		}
	}

}
