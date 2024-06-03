import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class CloudStorageApp {
    private static final int SERVER_PORT = 8888;
    private static final int CLIENT_PORT = 5678;
    private static final int BLOCK_SIZE = 4 * 1024 * 1024; // 4MB

    private DatagramSocket clientSocket;
    private InetAddress serverAddress;
    private String syncFolder;
    private Map<String, SyncStatus> syncStatusMap;
    private ExecutorService executorService;

    private WatchService watchService;

    public CloudStorageApp(String serverAddress, String syncFolder) throws IOException {
        this.serverAddress = InetAddress.getByName(serverAddress);
        this.syncFolder = syncFolder;
        this.syncStatusMap = new HashMap<>();
        this.clientSocket = new DatagramSocket(CLIENT_PORT);
        this.executorService = Executors.newCachedThreadPool();

        this.watchService = FileSystems.getDefault().newWatchService();
        Path path = Paths.get(syncFolder);
        path.register(watchService, StandardWatchEventKinds.ENTRY_DELETE);
    }

    public void start() {
        System.out.println("Cloud Storage Application started.");
        System.out.println("Sync Folder: " + syncFolder);
        System.out.println("Press Ctrl+C to exit.");

        // Start separate threads for file syncing, command transmission, and status monitoring
        executorService.execute(this::syncFiles);
        executorService.execute(this::handleCommands);
        executorService.execute(this::monitorStatus);
        executorService.execute(this::detectFileDeletion);
    }

    public void stop() {
        executorService.shutdownNow();
        clientSocket.close();
        System.out.println("Cloud Storage Application stopped.");
    }

    private void sendFileBlock(String fileName, byte[] blockData, int blockSize, int blockNumber) throws IOException {
        // Create a FileBlock object to hold the file block information
        FileBlock fileBlock = new FileBlock(fileName, blockNumber, blockData, blockSize);


        // Serialize the FileBlock object
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
        objectStream.writeObject(fileBlock);
        objectStream.flush();

        // Get the serialized file block data
        byte[] data = byteStream.toByteArray();

        // Create a UDP packet and send it to the server
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
        clientSocket.send(packet);

        objectStream.close();
        byteStream.close();
    }

    private void syncFiles() {
        // Continuously monitor the sync folder for changes and sync files as changes are detected
        File folder = new File(syncFolder);

        while (true) {
            File[] files = folder.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        String fileName = file.getName();
                        long fileSize = file.length();
                        long lastModified = file.lastModified();

                        // Check if file is in sync or needs synchronization
                        if (!isInSync(fileName, fileSize, lastModified)) {
                            System.out.println("Syncing file: " + fileName);

                            // Send file data in blocks
                            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                                byte[] buffer = new byte[BLOCK_SIZE];
                                int bytesRead;
                                int totalBytesRead = 0;

//                                int blockNumber = 0;
//
//                                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
//                                    // Send file block
//                                    sendFileBlock(fileName, Arrays.copyOfRange(buffer, 0, bytesRead), bytesRead, blockNumber);
//                                    totalBytesRead += bytesRead;
//                                    blockNumber++;
//                                }

                                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                                    // Send file data in blocks
                                    int blockNumber = (int) Math.ceil((double) totalBytesRead / BLOCK_SIZE);
                                    sendFileBlock(fileName, Arrays.copyOfRange(buffer, 0, bytesRead), bytesRead, blockNumber);
                                    totalBytesRead += bytesRead;
                                }

                                // Update sync status
                                updateSyncStatus(fileName, fileSize, lastModified, true);
                                System.out.println("File synced: " + fileName);
                            } catch (IOException e) {
                                System.out.println("Error syncing file: " + e.getMessage());
                            }
                        }
                    }
                }
            }

            // Sleep for a specified interval before checking for changes again
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("Sync process interrupted: " + e.getMessage());
            }
        }
    }

    private boolean isInSync(String fileName, long fileSize, long lastModified) {
        SyncStatus syncStatus = syncStatusMap.get(fileName);

        return syncStatus != null && syncStatus.fileSize == fileSize && syncStatus.lastModified == lastModified;
    }

    private void updateSyncStatus(String fileName, long fileSize, long lastModified, boolean inSync) {
        SyncStatus syncStatus = new SyncStatus(fileSize, lastModified, inSync);
        syncStatusMap.put(fileName, syncStatus);
    }

    private void handleCommands() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                clientSocket.receive(packet);

                String response = new String(packet.getData(), 0, packet.getLength());
                // Process the command received from the server (e.g., delete file, update file)
                String[] commandParts = response.split(" ");

                if (commandParts.length >= 2) {
                    String command = commandParts[0];
                    String fileName = commandParts[1];

                    switch (command) {
                        case "DELETE":
                            deleteFile(fileName);
                            break;
                        case "UPDATE":
                            updateFile(fileName);
                            break;
                        default:
                            System.out.println("Invalid command received: " + response);
                            break;
                    }
                } else {
                    System.out.println("Invalid command received: " + response);
                }
            } catch (IOException e) {
                System.out.println("Error receiving command: " + e.getMessage());
            }
        }
    }

    private void detectFileDeletion() {
        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                break;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path deletedFile = pathEvent.context();

                    // Print the deletion message
                    System.out.println("File deleted: " + deletedFile);

                    // Send delete request to the server
                    sendDeleteRequest(deletedFile.toString());
                }
            }

            key.reset();
        }
    }

    private void sendDeleteRequest(String fileName) {
        String deleteCommand = "DELETE " + fileName;
        byte[] commandBytes = deleteCommand.getBytes();

        try {
            DatagramPacket packet = new DatagramPacket(commandBytes, commandBytes.length, serverAddress, SERVER_PORT);
            clientSocket.send(packet);
        } catch (IOException e) {
            System.out.println("Error sending delete request: " + e.getMessage());
        }
    }

    private void deleteFile(String fileName) {
        File file = new File(syncFolder + File.separator + fileName);
        if (file.delete()) {
            syncStatusMap.remove(fileName);
            System.out.println("File deleted: " + fileName);
        } else {
            System.out.println("Error deleting file: " + fileName);
        }
    }

    private void updateFile(String fileName) {
        File file = new File(syncFolder + File.separator + fileName);
        if (file.exists()) {
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                sendFileUpdate(fileData, fileName);
                System.out.println("File updated: " + fileName);
            } catch (IOException e) {
                System.out.println("Error reading file: " + fileName);
            }
        } else {
            System.out.println("File not found: " + fileName);
        }
    }

    private void sendFileUpdate(byte[] fileData, String fileName) {
        int totalBlocks = (int) Math.ceil((double) fileData.length / BLOCK_SIZE);

        for (int blockNumber = 0; blockNumber < totalBlocks; blockNumber++) {
            int startIndex = blockNumber * BLOCK_SIZE;
            int endIndex = Math.min(startIndex + BLOCK_SIZE, fileData.length);
            byte[] blockData = Arrays.copyOfRange(fileData, startIndex, endIndex);

            DatagramPacket packet = new DatagramPacket(blockData, blockData.length, serverAddress, SERVER_PORT);
            try {
                clientSocket.send(packet);
            } catch (IOException e) {
                System.out.println("Error sending file update: " + fileName);
                break;
            }
        }
    }

    private void monitorStatus() {
        while (!Thread.currentThread().isInterrupted()) {
            for (Map.Entry<String, SyncStatus> entry : syncStatusMap.entrySet()) {
                String fileName = entry.getKey();
                SyncStatus syncStatus = entry.getValue();

                System.out.println("File: " + fileName +
                        " | In Sync: " + syncStatus.inSync +
                        " | File Size: " + syncStatus.fileSize +
                        " | Last Modified: " + new Date(syncStatus.lastModified));
            }

            try {
                Thread.sleep(10000); // Sleep for 10 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class SyncStatus {
        private long fileSize;
        private long lastModified;
        private boolean inSync;

        public SyncStatus(long fileSize, long lastModified, boolean inSync) {
            this.fileSize = fileSize;
            this.lastModified = lastModified;
            this.inSync = inSync;
        }
    }

    public static void main(String[] args) {
        try {
            CloudStorageApp cloudStorageApp = new CloudStorageApp("127.0.0.1", "/Users/darshansavaliya/Desktop/Summer2023/ASE/UpdateProject/CloudStorage/SyncFolder");
            cloudStorageApp.start();
        } catch (IOException e) {
            System.out.println("Error starting the Cloud Storage Application: " + e.getMessage());
        }
    }
}