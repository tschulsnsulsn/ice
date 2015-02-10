/*******************************************************************************
 * Copyright (c) 2015- UT-Battelle, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Initial API and implementation and/or initial documentation - 
 *   Jordan Deyton
 *******************************************************************************/
package org.eclipse.ice.viz.service.connections;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ice.datastructures.ICEObject.ICEObject;
import org.eclipse.ice.datastructures.ICEObject.IUpdateableListener;
import org.eclipse.ice.datastructures.form.Entry;

/**
 * This class provides an adapter that wraps a connection. It provides feedback
 * based on the current {@link #state} of the connection.
 * <p>
 * There are certain properties of a connection that are managed by this class:
 * <ol>
 * <li>{@code key} - The key or name associated with the connection.</li>
 * <li>{@link #connection} - The connection object.</li>
 * <li>{@link #connectionProperties properties} - A map of properties required
 * to open or maintain a connection. This is handled by sub-classes.</li>
 * <li>{@link #state} - The current state of the connection.</li>
 * <li>{@code listeners} - Listeners registered with the adapter are notified
 * when the connection state changes.</li>
 * </ol>
 * 
 * This class sub-classes {@link ICEObject}, which manages the key and the
 * listeners. However, the key is only ever set via
 * {@link #setConnectionProperties(List)}.
 * </p>
 * 
 * @author Jordan Deyton
 *
 * @param <T>
 *            The type of the connection object.
 */
public abstract class ConnectionAdapter<T> extends ICEObject {

	/**
	 * The current connection managed by this adapter.
	 */
	private T connection;
	/**
	 * The map of properties required to establish the {@link #connection}.
	 */
	private final Map<String, String> connectionProperties;
	/**
	 * The current state of the connection.
	 */
	private ConnectionState state;

	/**
	 * The default constructor.
	 */
	public ConnectionAdapter() {
		// Every connection starts off disconnected.
		state = ConnectionState.Disconnected;

		// Initialize the map of connection properties.
		connectionProperties = new HashMap<String, String>();
	}

	// ---- Connect, Disconnect, and implementations. ---- //
	/**
	 * Connects to the associated {@link #connection} if not already connected
	 * or connecting.
	 * <p>
	 * <b>Note:</b> This method is <i>not</i> intended to be overridden by
	 * sub-classes.
	 * </p>
	 *
	 * @param block
	 *            If true, then the calling thread will be blocked until the
	 *            connection succeeds or fails. If false, then the connection
	 *            process will not block the calling thread.
	 * @return True if the connection is established upon returning, false
	 *         otherwise.
	 */
	public boolean connect(boolean block) {
		boolean connected = false;

		String key = getKey();

		System.out.println("ConnectionAdapter message: "
				+ "Attempting to connect to \"" + key
				+ "\". The calling thread will " + (block ? "" : "not ")
				+ "be blocked.");

		if (state == ConnectionState.Connected) {
			connected = true;

			System.out.println("ConnectionAdapter message: " + "Connection \""
					+ key + "\" is already connected.");

		} else if (state != ConnectionState.Connecting) {

			System.out.println("ConnectionAdapter message: " + "Connection \""
					+ key + "\" is not connected.");

			// Create a new thread to open the connection.
			Thread thread = new Thread() {
				@Override
				public void run() {
					// Update the state.
					setState(ConnectionState.Connecting);

					// Try to open the connection.
					connection = openConnection();

					// Whether successful or not, update the state.
					if (connection != null) {
						setState(ConnectionState.Connected);
					} else {
						setState(ConnectionState.Failed);
					}
				}
			};
			thread.start();

			// If required, we must wait until the thread finishes. When that
			// happens, the connection should be open.
			if (block) {
				try {
					thread.join();
					// The connection is now open.
					connected = (state == ConnectionState.Connected);
				} catch (InterruptedException e) {
					// In the event the thread has an exception, show an error
					// and carry on.
					System.err.println("ConnectionAdapter error: "
							+ "Thread exception while opening connection \""
							+ key + "\".");
					e.printStackTrace();
				}
			}
		}

		return connected;
	}

	/**
	 * Opens a connection based on current settings in the adapter.
	 * <p>
	 * Implementations may assume that this thread is called from a worker
	 * thread.
	 * </p>
	 *
	 * @return The newly-opened connection, or {@code null} if the connection
	 *         could not be opened.
	 */
	protected abstract T openConnection();

	/**
	 * Disconnects the associated {@link #connection} if not already
	 * disconnected.
	 * <p>
	 * <b>Note:</b> This method is <i>not</i> intended to be overridden by
	 * sub-classes.
	 * </p>
	 *
	 * @param block
	 *            If true, then the calling thread will be blocked until the
	 *            disconnection succeeds or fails. If false, then the
	 *            disconnection process will not block the calling thread.
	 * @return True if the connection is closed upon returning, false otherwise.
	 */
	public boolean disconnect(boolean block) {
		boolean connected = false;

		String key = getKey();

		System.out.println("ConnectionAdapter message: "
				+ "Attempting to disconnect from \"" + key
				+ "\". The calling thread will " + (block ? "" : "not ")
				+ "be blocked.");

		if (state == ConnectionState.Connected) {
			connected = true;

			System.out.println("ConnectionAdapter message: " + "Connection \""
					+ key + "\" is connected. It will be disconnected.");

			// Create a new thread to close the connection.
			Thread thread = new Thread() {
				@Override
				public void run() {
					// Try to close the connection. If successful, update the
					// state.
					if (closeConnection(connection)) {
						setState(ConnectionState.Disconnected);
						connection = null;
					}
				}
			};
			// We do not want a daemon thread when closing... we want the
			// connection to close gracefully!
			thread.setDaemon(false);
			thread.start();

			// If required, we must wait until the thread finishes. When that
			// happens, the connection should be closed.
			if (block) {
				try {
					thread.join();
					// The connection is now closed.
					connected = (state == ConnectionState.Disconnected);
				} catch (InterruptedException e) {
					// In the event the thread has an exception, show an error
					// and carry on.
					System.err.println("ConnectionAdapter error: "
							+ "Thread exception while closing connection \""
							+ key + "\".");
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("ConnectionAdapter message: " + "Connection \""
					+ key + "\" is already disconnected.");
		}

		return !connected;
	}

	/**
	 * Closes the connection passed in as an argument. The adapter's current
	 * {@link #connection} will automatically be set to {@code null} after this
	 * operation completes.
	 * <p>
	 * Implementations may assume that this thread is called from a worker
	 * thread.
	 * </p>
	 *
	 * @param connection
	 *            The current connection to close.
	 * @return True if the connection was closed, false otherwise.
	 */
	protected abstract boolean closeConnection(T connection);

	// --------------------------------------------------- //

	// ---- Protected Methods (intended only for sub-classes) ---- //

	/**
	 * Gets the map of required connection properties. This should be used to
	 * update or read the properties when updating via
	 * {@link #setConnectionProperties(List)} or connecting via
	 * {@link #openConnection()}.
	 * 
	 * @return The map of required connection properties.
	 */
	protected Map<String, String> getConnectionProperties() {
		return connectionProperties;
	}

	/**
	 * This is a convenience method that updates the property stored in the
	 * {@link #connectionProperties} map and returns true if the value changed.
	 * 
	 * @param key
	 *            The key or ID of the property to change.
	 * @param value
	 *            The new value of the property.
	 * @return True if the value changed, false otherwise.
	 */
	protected boolean setConnectionProperty(String key, String value) {
		String oldValue = connectionProperties.put(key, value);
		return !(value != null ? value.equals(oldValue) : oldValue == null);
	}

	// ----------------------------------------------------------- //

	/**
	 * Sets the current connection state and notifies listeners of these
	 * changes.
	 * 
	 * @param state
	 *            The new connection state. If the same as before, nothing is
	 *            done.
	 */
	private void setState(ConnectionState state) {
		if (state != this.state) {
			System.out.println("ConnectionAdapter message: " + "Connection \""
					+ getKey() + "\" is now " + state + ".");
			this.state = state;
			notifyListeners();
		}
	}

	// ---- Public Methods ---- //
	/**
	 * Gets the connection managed by this adapter.
	 * 
	 * @return The associated connection.
	 */
	public T getConnection() {
		return connection;
	}

	/**
	 * Gets the connection property corresponding to the specified key.
	 * 
	 * @param key
	 *            The key or ID of the required connection property.
	 * @return The value of the connection property, or {@code null} if the key
	 *         did not exist.
	 */
	public String getConnectionProperty(String key) {
		return connectionProperties.get(key);
	}

	/**
	 * Gets the key currently associated with this connection. The value is
	 * usually maintained by a connection manager.
	 * 
	 * @return The connection key.
	 */
	public String getKey() {
		return getName();
	}

	/**
	 * Gets the current state of the associated connection.
	 * 
	 * @return The current state of the associated connection.
	 */
	public ConnectionState getState() {
		return state;
	}

	/**
	 * Sets the connection's required {@link #connectionProperties properties}
	 * based on the provided list of {@code Entry}s (usually a row from a
	 * {@link ConnectionManager}).
	 * <p>
	 * The associated connection will <i>not</i> be reset if the connection
	 * properties have changed.
	 * </p>
	 * 
	 * @param properties
	 *            The list of new connection properties.
	 * @return True if the new connection properties were valid and a change
	 *         occurred, false otherwise.
	 */
	public abstract boolean setConnectionProperties(List<Entry> properties);

	// TODO Remove this old code
	// public boolean setConnectionProperties(List<Entry> properties) {
	// // Store the old key and ID.
	// String oldKey = getKey();
	// int oldId = getId();
	//
	// System.out.println("ConnectionAdapter message: "
	// + "The connection properties for \"" + oldKey
	// + "\" are being updated.");
	//
	// // Update the properties. Use the sub-class' implementation.
	// boolean changed = updateProperties(properties, connectionProperties);
	// if (changed) {
	// System.out.println("ConnectionAdapter message: "
	// + "The connection properties for \"" + oldKey
	// + "\" changed. The connection will be reset.");
	//
	// // Temporarily reset to the old key and ID.
	// final String newKey = getKey();
	// final int newId = getId();
	// objectName = oldKey;
	// uniqueId = oldId;
	//
	// // Create a new thread to disconnect from the old connection and
	// // reconnect to the new one.
	// Thread thread = new Thread() {
	// @Override
	// public void run() {
	//
	// // Disconnect based on the old connection settings.
	// disconnect(true);
	//
	// // Restore the new key and ID.
	// objectName = newKey;
	// uniqueId = newId;
	//
	// // Re-connect based on the new connection settings.
	// connect(true);
	// }
	// };
	// thread.start();
	// }
	// return changed;
	// }
	//
	// /**
	// * Updates the connection's required properties based on the new input
	// * properties and notifies the caller if the properties changed.
	// *
	// * @param newProperties
	// * The list of new connection properties (usually a row from a
	// * {@link ConnectionManager}).
	// * @param propertyMap
	// * The map of required connection properties. This should be
	// * updated based on the list of new properties.
	// * @return True if the new changes require a
	// */
	// protected abstract boolean updateProperties(List<Entry> newProperties,
	// Map<String, String> propertyMap);
	// ------------------------ //

	// ---- Extends ICEObject ---- //
	// Certain functionality of ICEObject should be overridden. The adapter's
	// properties (ID, name, description) should not be set except through
	// setting the connection properties. Also, listeners should not be
	// registered more than once.
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ice.datastructures.ICEObject.ICEObject#register(org.eclipse
	 * .ice.datastructures.ICEObject.IUpdateableListener)
	 */
	public void register(IUpdateableListener listener) {
		// Don't allow double registration.
		if (!listeners.contains(listener)) {
			super.register(listener);
		}
	}

	/**
	 * Does nothing. The ID should be set as a connection property via
	 * {@link #setConnectionProperties(List)}.
	 */
	public void setId(int id) {
		return;
	}

	/**
	 * Does nothing. The name (or key) should be set as a connection property
	 * via {@link #setConnectionProperties(List)}.
	 */
	public void setName(String name) {
		return;
	}

	/**
	 * Does nothing. The description should be set as a connection property via
	 * {@link #setConnectionProperties(List)}.
	 */
	public void setDescription(String description) {
		return;
	}
	// --------------------------- //
}