package org.smartboot.socket.util;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;

/**
 * @author Seer
 * @version V1.0 , 2017/9/15
 */
public class FileBuffer {
    public static void main(String[] args) throws IOException {
        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(Paths.get(null));
//        fileChannel.write()
    }
}
