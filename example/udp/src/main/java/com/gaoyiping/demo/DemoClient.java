package com.gaoyiping.demo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DemoClient implements MessageProcessor<byte[]> {
//	private HashMap<String, AioSession> clients = new HashMap<String, AioSession>();
//	private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(12);

	public void process(AioSession session, byte[] msg) {
//		JSONObject jsonObject = JSON.parseObject(msg, JSONObject.class);
		System.out.println(new String(msg));
	}

	public void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
		// when connection state changed.
		switch (stateMachineEnum) {
			case NEW_SESSION:
				System.out.println("StateMachineEnum.NEW_SESSION");
				break;
			case INPUT_SHUTDOWN:
				System.out.println("StateMachineEnum.INPUT_SHUTDOWN");
				break;
			case PROCESS_EXCEPTION:
				System.out.println("StateMachineEnum.PROCESS_EXCEPTION");
				break;
			case DECODE_EXCEPTION:
				System.out.println("StateMachineEnum.DECODE_EXCEPTION");
				break;
			case INPUT_EXCEPTION:
				System.out.println("StateMachineEnum.INPUT_EXCEPTION");
				break;
			case OUTPUT_EXCEPTION:
				System.out.println("StateMachineEnum.OUTPUT_EXCEPTION");
				break;
			case SESSION_CLOSING:
				System.out.println("StateMachineEnum.SESSION_CLOSING");
				break;
			case SESSION_CLOSED:
				System.out.println("StateMachineEnum.SESSION_CLOSED");
				break;
//			case FLOW_LIMIT:
//				System.out.println("StateMachineEnum.FLOW_LIMIT");
//				break;
//			case RELEASE_FLOW_LIMIT:
//				System.out.println("StateMachineEnum.RELEASE_FLOW_LIMIT");
//				break;
			default:
				System.out.println("StateMachineEnum.default");
		}
	}

}
