package net.vinote.smart.socket.protocol;

import net.vinote.smart.socket.io.Channel;
import net.vinote.smart.socket.service.Session;
import net.vinote.smart.socket.service.process.AbstractServerDataGroupProcessor;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author Seer
 */
public final class HttpServerMessageProcessor extends AbstractServerDataGroupProcessor<HttpEntity> {

    @Override
    public void process(Session<HttpEntity> session, HttpEntity entry) throws Exception {
//        for (String key : entry.getHeadMap().keySet()) {
//            System.out.println(key + ": " + entry.getHeadMap().get(key));
//        }
//        byte[] data = new byte[1024];
//        InputStream in = entry.getBodyStream();
//        int length = 0;
//        while ((length = in.read(data)) != -1) {
//            System.out.print(new String(data, 0, length));
//        }
        session.sendWithoutResponse(entry);
//        System.out.println("body");
    }

    @Override
    public Session<HttpEntity> initSession(Channel<HttpEntity> session) {
        return new HttpSession(session);
    }
}
