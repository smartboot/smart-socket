import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

/**
 * @author 三刀
 * @version V1.0 , 2017/8/23
 */
public class BytesClientProcessor implements MessageProcessor<byte[]> {
    public static long packageCount = 0;
    private AioSession<byte[]> session;

    @Override
    public void process(AioSession<byte[]> session, byte[] msg) {
//        System.out.println("receive data from server�?" + Arrays.toString(msg));
        packageCount += 1;
    }

    @Override
    public void stateEvent(AioSession<byte[]> session, StateMachineEnum stateMachineEnum,
                           Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
                this.session = session;
                break;
            default:
                System.out.println("other state:" + stateMachineEnum);
        }

    }

    public AioSession<byte[]> getSession() {
        return session;
    }
}
