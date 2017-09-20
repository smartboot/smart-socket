package org.smartboot.socket.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author Seer
 * @version V1.0 , 2017/9/15
 */
public class FileBuffer {
    private static final Logger LOGGER = LogManager.getLogger(FileBuffer.class);
    private int readBlock = 0;
    private int readPosition = 0;
    private int writeBlock = 0;
    private int writePosition = 0;
    private int readCount;
    private int writeCount;
    private File readFile;
    private BufferedRandomAccessFile readAccessFile;
    private BufferedRandomAccessFile writeAccessFile;
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

        int fileSize = (int) readFileChannel.size();
        ByteBuffer buffer = ByteBuffer.allocate((fileSize - readPosition > 32 * 1024) ? 32 * 1024 : (fileSize - readPosition));
        while (buffer.hasRemaining()) {
            readFileChannel.read(buffer);
        }

        buffer.flip();
        readPosition += buffer.remaining();
        readCount += buffer.remaining();
        if (readPosition >= fileSize && readBlock < writeBlock) {
            readFileChannel.close();
            readAccessFile.close();
//            readFile.delete();
        }
        return buffer;
    }

    public void write(ByteBuffer buffer) throws IOException {
        if (writeFileChannel == null || !writeFileChannel.isOpen()) {
            openWriteFile();
        }
//        MappedByteBuffer mappedByteBuffer = writeFileChannel.map(FileChannel.MapMode.READ_WRITE, writePosition, buffer.remaining());
//        mappedByteBuffer.put(buffer);
        int writeSize = buffer.remaining();
        //不足MAX_LENGTH,全部输出
        if (writePosition + writeSize <= MAX_LENGTH) {
            while (buffer.hasRemaining()) {
                writeFileChannel.write(buffer);
            }
        } else {
            writeSize = MAX_LENGTH - writePosition;
            ByteBuffer subBuffer = ByteBuffer.allocate(writeSize);
            while (subBuffer.hasRemaining()) {
                subBuffer.put(buffer.get());
            }
            subBuffer.flip();
            writeFileChannel.write(subBuffer);
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
        writeAccessFile = new BufferedRandomAccessFile(mainTitle + (writeBlock++) + ".bin", "rw");
        writeFileChannel = writeAccessFile.getChannel();
        writePosition = 0;
    }

    private void openReadFile() throws IOException {
        readFile = new File(mainTitle + (readBlock++) + ".bin");
        readAccessFile = new BufferedRandomAccessFile(readFile, "r");
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
