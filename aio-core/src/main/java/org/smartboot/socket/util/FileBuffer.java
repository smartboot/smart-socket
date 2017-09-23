package org.smartboot.socket.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author Seer
 * @version V1.0 , 2017/9/15
 */
public class FileBuffer {
    private static final Logger LOGGER = LogManager.getLogger(FileBuffer.class);
    private volatile int readBlock = 0;
    private volatile int readPosition = 0;
    private volatile int writeBlock = 0;
    private volatile int writePosition = 0;
    private volatile int readCount;
    private volatile int writeCount;
    private File readFile;
    private RandomAccessFile readAccessFile;
    private RandomAccessFile writeAccessFile;
    private FileChannel readFileChannel;
    private FileChannel writeFileChannel;
    private static final int MAX_LENGTH = 32 * 1024 * 1024;
    private String mainTitle;

    public FileBuffer(String main) {
        this.mainTitle = main;
    }

    public ByteBuffer read() throws IOException {
        if (!hasRemaining()) {
            throw new RuntimeException("no data");
        }
        if (readFileChannel == null || !readFileChannel.isOpen()) {
            openReadFile();
        }

        int fileSize = readBlock < writeBlock ? MAX_LENGTH : writePosition;
        MappedByteBuffer mappedByteBuffer = readFileChannel.map(FileChannel.MapMode.READ_ONLY, readPosition, (fileSize - readPosition > 32 * 1024) ? 32 * 1024 : (fileSize - readPosition));
        if (!mappedByteBuffer.hasRemaining()) {
            System.out.println("file:" + fileSize + " , readB:" + readBlock + " ,p:" + readPosition + " ,writeB:" + writeBlock + " ,writeP:" + writePosition + ", readCount:" + readCount + " ,writeCount:" + writeCount);
            System.exit(0);
        }
        readPosition += mappedByteBuffer.remaining();
        readCount += mappedByteBuffer.remaining();
        if (readPosition >= fileSize && readBlock < writeBlock) {
            readFileChannel.close();
            readAccessFile.close();
            readFile.delete();
        }
        return mappedByteBuffer;
    }

    public void write(ByteBuffer buffer) throws IOException {
        if (writeFileChannel == null || !writeFileChannel.isOpen()) {
            openWriteFile();
        }
//        MappedByteBuffer mappedByteBuffer = writeFileChannel.map(FileChannel.MapMode.READ_WRITE, writePosition, buffer.remaining());
//        mappedByteBuffer.put(buffer);
        int writeSize = writePosition + buffer.remaining() <= MAX_LENGTH ? buffer.remaining() : MAX_LENGTH - writePosition;
        MappedByteBuffer mappedByteBuffer = writeFileChannel.map(FileChannel.MapMode.READ_WRITE, writePosition, writeSize);
        while (mappedByteBuffer.hasRemaining()) {
            mappedByteBuffer.put(buffer.get());
        }

        writePosition += writeSize;
        writeCount += writeSize;
        if (writePosition >= MAX_LENGTH) {
            writeFileChannel.close();
            writeAccessFile.close();
        }
        if (buffer.hasRemaining()) {
            write(buffer);
        }
    }

    private void openWriteFile() throws IOException {
        writeAccessFile = new RandomAccessFile(mainTitle + (writeBlock++) + ".bin", "rw");
        writeFileChannel = writeAccessFile.getChannel();
        writePosition = 0;
    }

    private void openReadFile() throws IOException {
        readFile = new File(mainTitle + (readBlock++) + ".bin");
        readAccessFile = new RandomAccessFile(readFile, "r");
        readFileChannel = readAccessFile.getChannel();
        readPosition = 0;
        LOGGER.info("open file " + readFile);
    }

    public void clear() {

    }

    public void close() {
        if (readFileChannel != null) {
            try {
                readFileChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (readAccessFile != null) {
            try {
                readAccessFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (writeFileChannel != null) {
            try {
                writeFileChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (writeAccessFile != null) {
            try {
                writeAccessFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean hasRemaining() {
        return readCount != writeCount;
    }

    public static void main(String[] args) throws IOException {
        FileBuffer fileBuffer = new FileBuffer("/Users/zhengjunwei/logs/fileBuffer");
        int i = 0;
        while (i++ < 100) {
            ByteBuffer b = ByteBuffer.wrap("HelloWOrld".getBytes());
            fileBuffer.write(b);
        }
        while (fileBuffer.hasRemaining()) {
            System.out.println(fileBuffer.read());
        }
        fileBuffer.close();
    }
}
