/*******************************************************************************
 * Copyright (c) 2014- UT-Battelle, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Initial API and implementation and/or initial documentation -
 *   Alexander J. McCaskey
 *******************************************************************************/
package org.eclipse.ice.item.nuclear;

import java.io.File;
import java.io.PrintStream;
import java.util.Dictionary;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.ice.datastructures.form.FormStatus;
import org.eclipse.ice.item.action.RemoteAction;
import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.remote.core.IRemoteFileService;
import org.eclipse.remote.core.IRemoteProcess;
import org.eclipse.remote.core.IRemoteProcessBuilder;
import org.eclipse.remote.core.IRemoteProcessService;
import org.eclipse.remote.core.exception.RemoteConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class serves as a utility for generating MOOSE YAML and Action Syntax
 * files on a remote machine and using them to construct the MOOSE input tree
 * locally in ICE.
 *
 * @author Alex McCaskey
 *
 */
public class RemoteYamlSyntaxGenerator extends RemoteAction {

	/**
	 * Logger for handling event messages and other information.
	 */
	private static final Logger logger = LoggerFactory.getLogger(RemoteYamlSyntaxGenerator.class);

	/**
	 * This method generates the YAML and Action syntax files in the local
	 * projectSpace/MOOSE directory from a remotely hosted MOOSE-based
	 * application. Clients of this Action must provide a Dictionary with 
	 * the mooseAppPath, mooseFolderPath, and remoteHost key/value pairs. 
	 *
	 * @param monitor
	 */
	public FormStatus execute(Dictionary<String, String> map) {

		// Local Declarations
		String appPath = map.get("mooseAppPath");
		String mooseFolderPath = map.get("mooseFolderPath");
		String hostName = map.get("remoteHost");
		if (appPath == null || mooseFolderPath == null) {
			logger.error("Invalid parameters passed to the RemoteYamlSyntaxGenerator Action.");
			return FormStatus.InfoError;
		}
		File appFile = new File(appPath);
		IRemoteProcessService processService = null;
		connection = getRemoteConnection(hostName);
		
		// Validate the connection we have. 
		if (connection == null) {
			logger.error("Could not create a remote connection in the RemoteYamlSyntaxGenerator.");
			return FormStatus.InfoError;
		}
		
		// Try to open the connection and fail if it will not open
		try {
			connection.open(null);
		} catch (RemoteConnectionException e) {
			// Print diagnostic information and fail
			logger.error(getClass().getName() + " Exception!", e);
			return FormStatus.InfoError;
		}

		// Do the upload(s) and launch the job if the connection is open
		if (connection.isOpen()) {
			// Diagnostic info
			logger.info("RemoteYamlSyntaxGenerator Message:" + " PTP connection established.");

			try {
				// Local Strings
				String remoteSeparator = connection.getProperty(IRemoteConnection.FILE_SEPARATOR_PROPERTY);
				String script = remoteSeparator + "tmp" + remoteSeparator + "executeYamlSyntax_" + System.getProperty("user.name");
				String fileLoc = remoteSeparator + "tmp" + remoteSeparator + appFile.getName();
				
				// Get the remote file system
				IRemoteFileService fileManager = connection.getService(IRemoteFileService.class);
				
				// Create a new script in /tmp
				IFileStore tmp = fileManager.getResource(script);
				PrintStream p = new PrintStream(tmp.openOutputStream(EFS.NONE, null));
				p.println("#!/bin/bash");
				p.println(appPath + " --yaml &> " + fileLoc + ".yaml");
				p.println(appPath + " --syntax &> " + fileLoc + ".syntax");
				p.close();
				
				// Make it executable
				IFileInfo info = tmp.fetchInfo();
				info.setAttribute(EFS.ATTRIBUTE_EXECUTABLE, true);
				tmp.putInfo(info, EFS.SET_ATTRIBUTES, null);

				// Get the IRemoteProcessService
				// and create the process builder to execute the script
				processService = connection.getService(IRemoteProcessService.class);
				IRemoteProcessBuilder pb = processService.getProcessBuilder(script);
				IRemoteProcess process = pb.start(IRemoteProcessBuilder.FORWARD_X11);

				// Wait til it's done
				while (!process.isCompleted()) {
					try {
						Thread.currentThread();
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// Complain
						logger.error(getClass().getName() + " Exception!", e);
					}
				}

				// Get a reference to the local MOOSE directory
				IFileStore localMooseFolder = EFS.getLocalFileSystem()
						.fromLocalFile(new File(mooseFolderPath));

				// Get a handle to the local file. Note that it may
				// not exist yet.
				IFileStore yamlStore = localMooseFolder.getChild(appFile.getName() + ".yaml");
				IFileStore actionStore = localMooseFolder.getChild(appFile.getName() + ".syntax");

				// Get the newly created remote files
				IFileStore remoteYaml = fileManager.getResource(fileLoc + ".yaml");
				IFileStore remoteAction = fileManager.getResource(fileLoc + ".syntax");

				// Copy the files from the remote machine to the
				// local machine.
				remoteYaml.copy(yamlStore, EFS.OVERWRITE, null);
				remoteAction.copy(actionStore, EFS.OVERWRITE, null);

				// Clean up by deleting the remote files
				tmp.delete(EFS.NONE, null);
				remoteYaml.delete(EFS.NONE, null);
				remoteAction.delete(EFS.NONE, null);

			} catch (Exception e) {
				logger.error("RemoteYamlSyntaxGenerator Error Message", e);
				return FormStatus.InfoError;
			}


		}

		return FormStatus.Processed;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ice.item.action.Action#cancel()
	 */
	@Override
	public FormStatus cancel() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ice.item.action.Action#getActionName()
	 */
	@Override
	public String getActionName() {
		return "Remote YAML/Syntax Generator";
	}

}
