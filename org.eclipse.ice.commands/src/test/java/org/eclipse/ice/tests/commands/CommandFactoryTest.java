/*******************************************************************************
 * Copyright (c) 2019- UT-Battelle, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Initial API and implementation and/or initial documentation - 
 *   Jay Jay Billings, Joe Osborn
 *******************************************************************************/
package org.eclipse.ice.tests.commands;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.eclipse.ice.commands.Command;
import org.eclipse.ice.commands.CommandConfiguration;
import org.eclipse.ice.commands.CommandFactory;
import org.eclipse.ice.commands.CommandStatus;
import org.eclipse.ice.commands.ConnectionAuthorizationHandler;
import org.eclipse.ice.commands.ConnectionAuthorizationHandlerFactory;
import org.eclipse.ice.commands.ConnectionConfiguration;
import org.eclipse.ice.commands.ConnectionManager;
import org.eclipse.ice.commands.ConnectionManagerFactory;
import org.eclipse.ice.commands.LocalFileHandler;
import org.eclipse.ice.commands.TxtFileConnectionAuthorizationHandler;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * This class tests {@link org.eclipse.ice.commands.CommandFactory}.
 * 
 * @author Jay Jay Billings, Joe Osborn
 *
 */
public class CommandFactoryTest {

	/**
	 * The hostname for which the job should run on. Default to local host name for
	 * now
	 */
	String hostname = getLocalHostname();

	/**
	 * Create a command factory to use for getting the commands.
	 */
	CommandFactory factory = new CommandFactory();

	/**
	 * A string containing the working directory for the executable to run in
	 */
	String workingDirectory;

	/**
	 * Get the present working directory to run things in
	 */
	String pwd = System.getProperty("user.dir") + "/src/test/java/org/eclipse/ice/tests/commands/";

	/**
	 * A connection configuration with which to test
	 */
	ConnectionConfiguration connectionConfig = new ConnectionConfiguration();

	/**
	 * A connection manager to manage the dummy connections for testing
	 */
	ConnectionManager manager = new ConnectionManager();

	/**
	 * Default constructor
	 */
	public CommandFactoryTest() {
	}

	/**
	 * Close the connections after we are finished with them in an individual test
	 * 
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		ConnectionManager manager = ConnectionManagerFactory.getConnectionManager();
		manager.removeAllConnections();
	}

	/**
	 * Run after the tests have finished processing. This function just removes the
	 * dummy text files that are created with log/error information from running
	 * various commands tests.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@AfterClass
	public static void tearDownAfterClass() throws IOException, InterruptedException {

		// Make and execute a simple command to remove the text files created
		// in these tests.

		// Make a string of all the output file names in this test
		// Didn't want to do *.txt, in the event that it inadvertently deletes other
		// files on people's computers
		String rm = "someLocalErrFile.txt someLocalOutFile.txt someLocalErrFileDir.txt someLocalOutFileDir.txt";
		rm += " someRemoteErrFile.txt someRemoteOutFile.txt someMultLocalErrFile.txt someMultLocalOutFile.txt";
		rm += " someLsOutFile.txt someLsErrFile.txt someMultRemoteOutFile.txt someMultRemoteErrFile.txt";
		rm += " somePythOutFile.txt somePythErrFile.txt someLsRemoteErrFile.txt someLsRemoteOutFile.txt";
		rm += " src/test/java/org/eclipse/ice/tests/someInputFile.txt src/test/java/org/eclipse/ice/tests/someOtherInputFile.txt";
		rm += " pythOutFile.txt pythErrFile.txt";
		ArrayList<String> command = new ArrayList<String>();
		// Build a command
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			command.add("powershell.exe");
		} else {
			command.add("/bin/bash");
			command.add("-c");
		}
		command.add("rm " + rm);
		// Execute the command with the process builder api
		ProcessBuilder builder = new ProcessBuilder(command);
		// Files exist in the top most directory of the package
		String topDir = System.getProperty("user.dir");
		File file = new File(topDir);
		builder.directory(file);
		// Process it
		Process job = builder.start();
		job.waitFor(); // wait for it to finish

		// Remove lal the connections that were opened in testing
		ConnectionManager manager = ConnectionManagerFactory.getConnectionManager();
		manager.removeAllConnections();

	}

	/**
	 * This function tests a multi-hop remote command, where the command logs into a
	 * remote host and then executes on a different remote host. TODO - Commented
	 * out for now since multi-hop command source code is not implemented and can be
	 * for future development.
	 */
	// @Test
	public void testMultiHopRemoteCommand() {
		fail("src not implemented");
		System.out.println("\n\n\nTesting a multi-hop remote command");
		// Set the CommandConfiguration class
		CommandConfiguration commandConfig = setupDefaultCommandConfig();
		commandConfig.setExecutable("./test_code_execution.sh");
		commandConfig.addInputFile("someInputFile", "someInputFile.txt");
		commandConfig.setAppendInput(true);
		commandConfig.setCommandId(99);
		commandConfig.setErrFileName("hopRemoteErrFile.txt");
		commandConfig.setOutFileName("hopRemoteOutFile.txt");
		commandConfig.setRemoteWorkingDirectory("/tmp/remoteCommandTestDirectory");
		// Just put in a dummy directory for now
		commandConfig.setWorkingDirectory("/home/user/somedirectory");

		// Set the connection configuration to a dummy remote connection
		// This is the connection where the job will be executed
		// Get a factory which determines the type of authorization
		ConnectionAuthorizationHandlerFactory authFactory = new ConnectionAuthorizationHandlerFactory();
		// Request a ConnectionAuthorization of type text file which contains the
		// credentials
		ConnectionAuthorizationHandler auth = authFactory.getConnectionAuthorizationHandler("text",
				"/tmp/ice-remote-creds.txt");
		// Set it
		connectionConfig.setAuthorization(auth);

		// Note the password can be input at the console by not setting the
		// the password explicitly in the connection configuration
		connectionConfig.setName("executeConnection");
		connectionConfig.deleteWorkingDirectory(true);

		ConnectionConfiguration intermConnection = new ConnectionConfiguration();
		// TODO - this will have to be changed to some other remote connection
		intermConnection.setAuthorization(auth);
		intermConnection.setName("intermediateConnection");
		intermConnection.deleteWorkingDirectory(false);

		// Get the command
		Command remoteCommand = null;
		try {
			remoteCommand = factory.getCommand(commandConfig, intermConnection, connectionConfig);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Run it
		CommandStatus status = remoteCommand.execute();

		// assert that it was successful
		assert (status == CommandStatus.SUCCESS);

		// Delete the connections
		manager.removeAllConnections();

	}

	/**
	 * This function tests a more boring command, e.g. just executing "ls" at the
	 * command prompt This shows that the API can be used to execute basic command
	 * line prompts.
	 */
	@Test
	public void testBoringCommandLocally() {
		System.out.println("Test boring command locally");
		// Make the command configuration
		CommandConfiguration cmdCfg = setupDefaultCommandConfig();
		cmdCfg.setExecutable("ls -lrt");

		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			// Add powershell interpeter if os is windows
			cmdCfg.setInterpreter("powershell.exe");
			// just use ls because powershell automatically adds the -lrt
			// and doesn't know what -lrt is anyway
			cmdCfg.setExecutable("ls");
		}
		cmdCfg.setNumProcs("1");
		cmdCfg.setInstallDirectory("");
		cmdCfg.setWorkingDirectory(pwd);
		cmdCfg.setAppendInput(false);
		cmdCfg.setCommandId(1);
		cmdCfg.setErrFileName("someLsErrFile.txt");
		cmdCfg.setOutFileName("someLsOutFile.txt");

		// Make the connection configuration
		ConnectionConfiguration ctCfg = new ConnectionConfiguration();
		ConnectionAuthorizationHandler handler = new TxtFileConnectionAuthorizationHandler();
		handler.setHostname(hostname);
		ctCfg.setAuthorization(handler);

		// Get and run the command
		Command cmd = null;
		try {
			cmd = factory.getCommand(cmdCfg, ctCfg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		CommandStatus status = cmd.execute();

		// Check that it properly finished
		assert (status == CommandStatus.SUCCESS);
		System.out.println("Finished boring command locally");
	}

	/**
	 * This function tests a more boring command, e.g. just executing "ls" at the
	 * command prompt This shows that the API can be used to execute basic command
	 * line prompts.
	 */
	@Test
	public void testBoringCommandRemotely() {
		System.out.println("Test remotely ls");
		// Setup the command configuration
		CommandConfiguration cmdCfg = setupDefaultCommandConfig();
		cmdCfg.setExecutable("ls -lrt");
		cmdCfg.setAppendInput(false);
		cmdCfg.setRemoteWorkingDirectory("/tmp/");
		cmdCfg.setCommandId(1);
		cmdCfg.setErrFileName("someLsRemoteErrFile.txt");
		cmdCfg.setOutFileName("someLsRemoteOutFile.txt");

		// Setup the connection configuration
		ConnectionConfiguration ctCfg = setupDummyConnectionConfiguration();
		ctCfg.deleteWorkingDirectory(false);
		// Get and run the command
		Command cmd = null;
		try {
			cmd = factory.getCommand(cmdCfg, ctCfg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		CommandStatus status = cmd.execute();

		// Assert that it finished correctly
		assert (status == CommandStatus.SUCCESS);
	}

	/**
	 * This function tests with real files to test an actual job processing. The job
	 * executes a script with some hello world commands in it.
	 */
	@Test
	public void testFunctionalLocalCommand() {
		System.out.println("Test functional local command");
		// Set some things specific to the local command
		CommandConfiguration commandConfig = setupDefaultCommandConfig();
		commandConfig.setExecutable("./test_code_execution.sh");
		// If it is windows, configure the test to run on windows
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			commandConfig.setExecutable(".\\test_code_execution.ps1");
			commandConfig.setInterpreter("powershell.exe");
		}
		commandConfig.addInputFile("someInputFile", "someInputFile.txt");
		commandConfig.setAppendInput(true);
		commandConfig.setCommandId(1);
		commandConfig.setErrFileName("someLocalErrFile.txt");
		commandConfig.setOutFileName("someLocalOutFile.txt");

		// Make a default boring connection authorization
		ConnectionAuthorizationHandler handler = new TxtFileConnectionAuthorizationHandler();
		handler.setHostname(hostname);
		connectionConfig.setAuthorization(handler);

		// Get the command
		Command localCommand = null;
		try {
			localCommand = factory.getCommand(commandConfig, connectionConfig);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Run it
		CommandStatus status = localCommand.execute();

		assert (status == CommandStatus.SUCCESS);
		System.out.println("Finished functional local command");
	}

	/**
	 * Test method for {@link org.eclipse.ice.commands.CommandFactory#getCommand()}
	 * and for the whole {@link org.eclipse.ice.commands.LocalCommand#execute()}
	 * execution chain with an incomplete Command dictionary. This function is
	 * intended to catch the "exception" catching of the API, where the exception is
	 * in quotes due to the return of a bad CommandStatus rather than e.g. a true
	 * java Exception.
	 */
	@Test
	public void testNonFunctionalLocalCommand() {

		System.out.println("\nTesting some commands where not enough command information was provided.");

		// Create a command configuration that doesn't have all the necessary
		// information
		// Set the CommandConfiguration class
		CommandConfiguration badCommandConfig = new CommandConfiguration();

		ConnectionConfiguration badConnectConfig = new ConnectionConfiguration();
		ConnectionAuthorizationHandler handler = new TxtFileConnectionAuthorizationHandler();
		handler.setHostname(hostname);
		badConnectConfig.setAuthorization(handler);
		// Get the command
		Command localCommand = null;
		try {
			localCommand = factory.getCommand(badCommandConfig, badConnectConfig);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Run it and expect that it fails
		CommandStatus status = localCommand.execute();

		assert (status == CommandStatus.INFOERROR);

	}

	/**
	 * Test method for {@link org.eclipse.ice.commands.CommandFactory#getCommand()}
	 * and for the whole {@link org.eclipse.ice.commands.LocalCommand#execute()}
	 * execution chain with an uncompleted Command dictionary. This function is
	 * intended to test some of the exception catching, thus it is expected to
	 * "fail."
	 */
	@Test
	public void testIncorrectWorkingDirectory() {
		/**
		 * Run another non functional command, with a non existing working directory
		 */

		System.out.println("\nTesting a command where a nonexistent working directory was provided.");

		CommandConfiguration commandConfig = setupDefaultCommandConfig();
		commandConfig.setExecutable("./test_code_execution.sh");
		commandConfig.addInputFile("someInputFile", "someInputFile.txt");

		commandConfig.setAppendInput(true);

		// Set the commandConfig class
		commandConfig.setCommandId(3);
		commandConfig.setErrFileName("someLocalErrFileDir.txt");
		commandConfig.setOutFileName("someLocalOutFileDir.txt");
		commandConfig.setWorkingDirectory("/tmp/some_nonexistent_directory");

		ConnectionAuthorizationHandler handler = new TxtFileConnectionAuthorizationHandler();
		handler.setHostname(hostname);
		connectionConfig.setAuthorization(handler);
		// Get the command
		Command localCommand2 = null;
		try {
			localCommand2 = factory.getCommand(commandConfig, connectionConfig);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Run it and expect that it fails
		CommandStatus status2 = localCommand2.execute();

		assert (status2 == CommandStatus.FAILED);
	}

	/**
	 * This function tests a functional remote command with the full command factory
	 * implementation
	 */
	@Test
	public void testFunctionalRemoteCommand() {

		System.out.println("\n\n\nTesting a functional remote command");

		// Set the CommandConfiguration class
		CommandConfiguration commandConfig = setupDefaultCommandConfig();

		commandConfig.setExecutable("./test_code_execution.sh");
		commandConfig.addInputFile("someInputFile", "someInputFile.txt");
		commandConfig.setAppendInput(true);
		commandConfig.setCommandId(4);
		commandConfig.setErrFileName("someRemoteErrFile.txt");
		commandConfig.setOutFileName("someRemoteOutFile.txt");
		commandConfig.setRemoteWorkingDirectory("/tmp/remoteCommandTestDirectory");

		// Get the connection configuration for remote testing
		ConnectionConfiguration config = setupDummyConnectionConfiguration();

		// Get the command
		Command remoteCommand = null;
		try {
			remoteCommand = factory.getCommand(commandConfig, config);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Run it
		CommandStatus status = remoteCommand.execute();

		// assert that it was successful
		assert (status == CommandStatus.SUCCESS);

		// Delete the connections
		manager.removeAllConnections();
		System.out.println("Finished remote functional command");

	}

	/**
	 * This tests a command which requires multiple input files to run remotely
	 */
	@Test
	public void testMultipleInputFilesRemotely() {
		System.out.println("Test multiple input files remotely");
		// Set the CommandConfiguration class

		CommandConfiguration commandConfig = setupDefaultCommandConfig();
		commandConfig.setExecutable("./test_code_execution.sh");
		commandConfig.addInputFile("someInputFile", "someInputFile.txt");
		commandConfig.setAppendInput(true);
		commandConfig.setCommandId(5);
		commandConfig.setErrFileName("someMultRemoteErrFile.txt");
		commandConfig.setOutFileName("someMultRemoteOutFile.txt");
		commandConfig.setRemoteWorkingDirectory("/tmp/remoteCommandTestDirectoryMult");
		// Add another input file to the list of input files already started
		commandConfig.addInputFile("someOtherFile", "someOtherInputFile.txt");

		// Get the connection configuration
		ConnectionConfiguration config = setupDummyConnectionConfiguration();
		// Get the command
		Command remoteCommand = null;
		try {
			remoteCommand = factory.getCommand(commandConfig, config);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Run it
		CommandStatus status = remoteCommand.execute();

		// assert that it was successful
		assert (status == CommandStatus.SUCCESS);
		System.out.println("Finished multiple input files remotely");
	}

	/**
	 * This tests a command which requires multiple input files to run locally
	 */
	@Test
	public void testMultipleInputFilesLocally() {
		System.out.println("test multiple input files locally");
		// Set some things specific to the local command

		CommandConfiguration commandConfig = setupDefaultCommandConfig();

		commandConfig.addInputFile("someInputFile", "someInputFile.txt");
		commandConfig.setCommandId(6);
		commandConfig.setErrFileName("someMultLocalErrFile.txt");
		commandConfig.setOutFileName("someMultLocalOutFile.txt");
		// Add another input file
		commandConfig.addInputFile("someOtherFile", "someOtherInputFile.txt");
		// Also tests case where append input is set to false and the arguments
		// are passed as variables as follows
		commandConfig.setExecutable("./test_code_execution.sh ${someInputFile} ${someOtherFile}");
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			commandConfig.setInterpreter("powershell.exe");
			commandConfig.setExecutable(".\\test_code_execution.ps1");
		}
		commandConfig.setAppendInput(false);
		ConnectionAuthorizationHandler handler = new TxtFileConnectionAuthorizationHandler();
		handler.setHostname(hostname);
		connectionConfig.setAuthorization(handler);

		// Get the command
		Command localCommand = null;
		try {
			localCommand = factory.getCommand(commandConfig, connectionConfig);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Run it
		CommandStatus status = localCommand.execute();

		assert (status == CommandStatus.SUCCESS);
		System.out.println("Finished testing multiple input files locally");
	}

	/**
	 * This function tests the processing of a hello world python script, rather
	 * than a bash script
	 */
	@Test
	public void testPythonScript() {

		System.out.println("Testing python script");
		// Get the present working directory
		String pwd = System.getProperty("user.dir");

		// Create the path relative to the current directory where the test script lives
		String scriptDir = pwd + "/src/test/java/org/eclipse/ice/tests/commands/";

		// Create a command configuration corresponding to a python script
		CommandConfiguration configuration = setupDefaultCommandConfig();
		configuration.setExecutable("test_python_script.py");
		configuration.setInterpreter("python");
		configuration.setCommandId(9);
		configuration.setErrFileName("somePythErrFile.txt");
		configuration.setOutFileName("somePythOutFile.txt");
		configuration.setAppendInput(true);
		configuration.addInputFile("inputfile", "someInputFile.txt");
		configuration.addInputFile("inputfile2", "someOtherInputFile.txt");
		configuration.setWorkingDirectory(scriptDir);
		configuration.setRemoteWorkingDirectory("/tmp/pythonTest");

		// Get the dummy connection configuration
		ConnectionConfiguration connectionConfig = setupDummyConnectionConfiguration();

		// Get the command and run it
		Command command = null;
		try {
			command = factory.getCommand(configuration, connectionConfig);
		} catch (IOException e) {
			e.printStackTrace();
		}

		CommandStatus status = command.execute();

		assert (status == CommandStatus.SUCCESS);
		System.out.println("finished python script test");
	}

	/**
	 * This function tests the execution of a command where the executable lives on
	 * the remote host and the input files live on the local host
	 */
	@Test
	public void testRemoteExecutableLocalInputFiles() {
		System.out.println("Testing command where files live on different hosts.");
		// Get the present working directory
		String pwd = System.getProperty("user.dir");

		// Create the path relative to the current directory where the test script lives
		String inputFileDir = pwd + "/src/test/java/org/eclipse/ice/tests/";

		// Copy the input files to this directory for this test. They are removed at the
		// end of the class. Useful for putting the input files in a different directory
		// in the repo from where the execution script lives.

		LocalFileHandler handler = new LocalFileHandler();
		handler.copy(inputFileDir + "commands/someInputFile.txt", inputFileDir);
		handler.copy(inputFileDir + "commands/someOtherInputFile.txt", inputFileDir);

		// Create a command configuration corresponding to a python script
		CommandConfiguration configuration = setupDefaultCommandConfig();
		// This path exists on the dummy host server
		configuration.setExecutable(
				"/opt/ice/org.eclipse.ice.commands/src/test/java/org/eclipse/ice/tests/commands/test_python_script.py");
		configuration.setInterpreter("python");
		configuration.setCommandId(9);
		configuration.setErrFileName("pythErrFile.txt");
		configuration.setOutFileName("pythOutFile.txt");
		configuration.setAppendInput(true);
		configuration.setWorkingDirectory(inputFileDir);
		configuration.addInputFile("inputfile", "someInputFile.txt");
		configuration.addInputFile("inputfile2", "someOtherInputFile.txt");
		configuration.setRemoteWorkingDirectory("/tmp/pythonTest");

		// Get the dummy connection configuration
		ConnectionConfiguration connectionConfig = setupDummyConnectionConfiguration();

		// Get the command and run it
		Command command = null;
		try {
			command = factory.getCommand(configuration, connectionConfig);
		} catch (IOException e) {
			e.printStackTrace();
		}

		CommandStatus status = command.execute();

		assert (status == CommandStatus.SUCCESS);
		System.out.println("finished python script test");

	}

	/**
	 * This function sets up and returns a default case of a command configuration
	 * for use throughout the tests.
	 * 
	 * @return
	 */
	private CommandConfiguration setupDefaultCommandConfig() {
		CommandConfiguration config = new CommandConfiguration();
		config.setNumProcs("1");
		config.setInstallDirectory("");
		config.setWorkingDirectory(pwd);
		config.setOS(System.getProperty("os.name"));

		return config;
	}

	/**
	 * A helper function to return the dummy connection configuration for remote
	 * testing
	 * 
	 * @return - dummy connection configuration
	 */
	private ConnectionConfiguration setupDummyConnectionConfiguration() {

		ConnectionConfiguration cfg = new ConnectionConfiguration();
		// Make the connection configuration
		// Get a factory which determines the type of authorization
		ConnectionAuthorizationHandlerFactory authFactory = new ConnectionAuthorizationHandlerFactory();
		// Request a ConnectionAuthorization of type text file which contains the
		// credentials
		String credFile = "/tmp/ice-remote-creds.txt";
		if (System.getProperty("os.name").toLowerCase().contains("win"))
			credFile = "C:\\Users\\Administrator\\ice-remote-creds.txt";
		ConnectionAuthorizationHandler auth = authFactory.getConnectionAuthorizationHandler("text", credFile);
		// Set it
		cfg.setAuthorization(auth);

		// Note the password can be input at the console by not setting the
		// the password explicitly in the connection configuration
		cfg.setName("dummyConnection");
		cfg.deleteWorkingDirectory(true);

		return cfg;
	}

	/**
	 * This function just returns the local hostname of your local computer. It is
	 * useful for testing a variety of local commands.
	 * 
	 * @return - String - local hostname
	 */
	protected static String getLocalHostname() {
		// Get the hostname for your local computer
		InetAddress addr = null;
		try {
			addr = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		String hostname = addr.getHostName();

		return hostname;
	}

}