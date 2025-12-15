package org.smartboot.socket.transport;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Frish2021
 * 用于输出banner
 */
final class Banner {
    private final IoServerConfig config;
    private final BannerFormat format;

    Banner(IoServerConfig config) {
        this.config = config;
        this.format = new BannerFormat(config);
    }

    void printBanner(String fileName, Consumer<IoServerConfig> defaultPrint) throws IOException {
        try(InputStream in = AioQuickServer.class.getClassLoader().getResourceAsStream(fileName)) {
            if (in == null) {
                defaultPrint.accept(config);
                return;
            }

            List<String> lines = IOUtil.readLineFromStream(in);
            lines.forEach(format::printf);
        }
    }
}
