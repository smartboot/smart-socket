import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;

/**
 * @author 三刀
 * @version V1.0 , 2017/8/23
 */
public class BytesServerProcessor implements MessageProcessor<byte[]> {
    @Override
    public void process(AioSession<byte[]> session, byte[] msg) {
        byte[] respBytes = msg.clone();
        respBytes[0] = (byte) ((respBytes[0] + 1) % 64);
//        System.out.println("receive data from client : " + Arrays.toString(msg));
//        System.out.println("receive data length : " + msg.length);
//        System.out.println("response data to client:" + Arrays.toString(respBytes));
        try {
            session.write(respBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stateEvent(AioSession<byte[]> session, StateMachineEnum stateMachineEnum,
                           Throwable throwable) {
        System.out.println(stateMachineEnum);
    }
}
