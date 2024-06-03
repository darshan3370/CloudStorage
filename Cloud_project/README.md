
## Cloud Storage Application

The Cloud Storage Application is a simplified cloud-based storage solution that allows users to sync files between multiple clients and a central server. It focuses on core functionalities and concurrent programming.

## Features

    File Sync: Continuous synchronization of files as changes are detected.
    Support for text and binary files of various sizes.
    Multiple file syncing for multiple clients simultaneously.
    Sync Status Monitoring: Reports the sync status of each file.
    Error Handling: Preserves data consistency in various situations, such as network failure or user errors.


## Dependencies

The Cloud Storage Application has the following dependencies:

    Java Development Kit (JDK) 8 or above.
    Java Socket API for network communication.

## Usage
	1. Download the source code: Download the source code as a ZIP file and extract it to a directory of your choice.
	
	2. Open the project in your preferred Java development environment (e.g., IntelliJ): Launch your Java IDE and import the project into the workspace. In IntelliJ, you can select "Open" and navigate to the project folder.
	
	3. Build the project to compile the source code: Before running the application, ensure that the project is successfully built. Most Java IDEs provide a built-in build or compile option. Use it to compile the source code and resolve any dependencies.
	
	4. Configure the server address and sync folder in the CloudStorageApp class: Open the CloudStorageApp.java file and locate the constructor. Provide the appropriate server address and the folder path that you want to sync. For example:

	Replace the folder path "C:/Users/YourUsername/SyncFolder" with the actual path to the folder you want to sync. (Both the Java File)
	
	5. Run the CloudStorageServer class to start the server: Locate the CloudStorageServer.java file in your IDE's project explorer. Right-click on it and select "Run" or use the corresponding option in your IDE's toolbar. This will start the server, and it will be ready to accept client connections.
	
	6. Run the CloudStorageApp class to start the client application: Locate the CloudStorageApp.java file in your project explorer. Right-click on it and select "Run" or use the corresponding option in your IDE's toolbar. This will start the client application and establish a connection with the server.

	7. Follow the command line prompts to interact with the application: The client application will display a set of command line prompts for performing various actions, such as adding files to the sync folders, monitoring sync status, or exiting the application. Follow the prompts and provide the required inputs to execute the desired actions.

	8. Monitor the sync status and file transfers in the console output: The client application and server will display relevant information in the console output. You can observe the sync status of each file, file transfers happening between the client and server, and any error or status messages generated during the application's execution.

       *******************************************************************************	
Make sure to have the server running before starting the client application to establish a connection. You can also run multiple instances of the client application on different machines or in separate IDE instances to test synchronization between multiple clients.
