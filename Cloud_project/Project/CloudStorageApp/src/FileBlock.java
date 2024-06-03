import java.io.Serializable;

public class FileBlock implements Serializable {
    private static final long serialVersionUID = 1L;
    private String fileName;
    private int blockNumber;
    private byte[] blockData;
    private int blockSize;

    public FileBlock(String fileName, int blockNumber, byte[] blockData, int blockSize) {
        this.fileName = fileName;
        this.blockNumber = blockNumber;
        this.blockData = blockData;
        this.blockSize = blockSize;
    }

    public String getFileName() {
        return fileName;
    }

    public int getBlockNumber() {
        return blockNumber;
    }

    public byte[] getBlockData() {
        return blockData;
    }

    public int getBlockSize() {
        return blockSize;
    }
}


