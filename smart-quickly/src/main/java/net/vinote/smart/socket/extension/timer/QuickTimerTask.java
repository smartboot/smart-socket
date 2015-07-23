package net.vinote.smart.socket.extension.timer;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import net.vinote.smart.socket.logger.RunLogger;

/**
 * 服务器定时任务
 * 
 * @author Seer
 *
 */
public abstract class QuickTimerTask extends TimerTask {
	private static final RunLogger logger = RunLogger.getLogger();
	private static Timer timer = new Timer("Quick Timer", true);

	public QuickTimerTask() {
		timer.schedule(this, getDelay(), getPeriod());
		logger.log(Level.SEVERE, "Regist QuickTimerTask---- "
				+ this.getClass().getSimpleName());
	}

	/**
	 * 获取定时任务的延迟启动时间
	 * 
	 * @return
	 */
	protected long getDelay() {
		return 0;
	}

	/**
	 * 获取定时任务的执行频率
	 * 
	 * @return
	 */
	protected abstract long getPeriod();

	public static void cancelQuickTask() {
		timer.cancel();
	}
}
