import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.ClassNotFoundException;

public class CloudStorageServer {
    private static final int SERVER_PORT = 8888;
    private static final int BLOCK_SIZE = 4 * 1024 * 1024; // 4MB
    private static final String STORAGE_FOLDER = "/Users/darshansavaliya/Desktop/Summer2023/ASE/UpdateProject/CloudStorage/ServerStorage/";

    private DatagramSocket serverSocket;

    public CloudStorageServer() throws IOException {
        this.serverSocket = new DatagramSocket(SERVER_PORT);
    }

    public void start() {
        System.out.println("Cloud Storage Server started.");
        System.out.println("Storage Folder: " + STORAGE_FOLDER);
        System.out.println("Press Ctrl+C to exit.");

        try {
            while (!Thread.currentThread().isInterrupted()) {
                byte[] buffer = new byte[BLOCK_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(packet);

                System.out.println("Processing File Block");

                // Process the received file block
                processFileBlock(packet.getData(), packet.getLength());
            }
        } catch (IOException e) {
            System.out.println("Cloud Storage Server error: " + e.getMessage());
        }

        serverSocket.close();
        System.out.println("Cloud Storage Server stopped.");
    }

    private FileBlock deserializeFileBlock(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        ObjectInputStream objectStream = new ObjectInputStream(byteStream);
        FileBlock fileBlock = (FileBlock) objectStream.readObject();
        objectStream.close();
        byteStream.close();
        return fileBlock;
    }

    private void processFileBlock(byte[] data, int length) {
        String request = new String(data, 0, length);

        // Check if the request contains the "DELETE" keyword
        if (request.toUpperCase().startsWith("DELETE")) {
            // Extract the filename from the request
            String[] parts = request.split(" ");
            if (parts.length == 2) {
                String fileName = parts[1];

                // Handle the delete request
                try {
                    deleteFileFromDisk(fileName);
                    System.out.println("File deleted: " + fileName);
                } catch (IOException e) {
                    System.out.println("Error deleting file: " + e.getMessage());
                }
                return;
            }
        }

        // Continue with deserialization if it's not a delete request
        FileBlock fileBlock;
        try {
            fileBlock = deserializeFileBlock(data);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error deserializing file block: " + e.getMessage());
            return;
        }

        String fileName = fileBlock.getFileName();
        byte[] fileContent = fileBlock.getBlockData();

        if (fileContent != null) {
            // Save the file content to disk
            int blockNumber = fileBlock.getBlockNumber();
            try {
                saveFileBlockToDisk(fileName, blockNumber, fileContent);
                System.out.println("File block saved: " + fileName);

                // Check if all blocks of the file have been received
                int totalBlocks = calculateTotalBlocks(fileName);
                if (blockNumber == totalBlocks - 1) {
                    System.out.println("File received: " + fileName);
                }
            } catch (IOException e) {
                System.out.println("Error saving file block: " + e.getMessage());
            }
        } else {
            System.out.println("Invalid file block: " + fileName);
        }
    }


    private void deleteFileFromDisk(String fileName) throws IOException {
        String filePath = STORAGE_FOLDER + fileName;
        Path path = Paths.get(filePath);
        Files.deleteIfExists(path);
    }


    private void saveFileBlockToDisk(String fileName, int blockNumber, byte[] blockData) throws IOException {
        String filePath = STORAGE_FOLDER + fileName;

        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
            fileOutputStream.write(blockData);
        }
    }

    private int calculateTotalBlocks(String fileName) {
        long fileSize = getFileSize(fileName);
        return (int) Math.ceil((double) fileSize / BLOCK_SIZE);
    }

    private long getFileSize(String fileName) {
        Path filePath = Paths.get(STORAGE_FOLDER + fileName);
        try {
            return Files.size(filePath);
        } catch (IOException e) {
            return 0;
        }
    }

    public static void main(String[] args) {
        try {
            CloudStorageServer cloudStorageServer = new CloudStorageServer();
            cloudStorageServer.start();
        } catch (IOException e) {
            System.out.println("Error starting the Cloud Storage Server: " + e.getMessage());
        }
    }
}