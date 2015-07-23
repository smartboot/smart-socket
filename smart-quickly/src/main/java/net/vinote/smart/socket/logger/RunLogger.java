package net.vinote.smart.socket.logger;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.vinote.smart.socket.lang.StringUtils;

public class RunLogger {

	private static RunLogger runlogger;

	private Logger logger = null;;

	private LoggerConfig config;

	private boolean enabled = true;

	private RunLogger() {
		logger = Logger.getLogger(RunLogger.class.getName());
		logger.setUseParentHandlers(false);
		logger.getParent().setLevel(Level.ALL);
	}

	public static RunLogger getLogger() {
		if (runlogger == null) {
			synchronized (RunLogger.class) {
				if (runlogger == null) {
					runlogger = new RunLogger();
				}
			}
		}
		return runlogger;
	}

	public synchronized void init(LoggerConfig config) {
		try {
			// 移除已注册的Handler
			Handler[] handls = logger.getHandlers();
			if (handls != null) {
				for (Handler h : handls) {
					logger.removeHandler(h);
				}
			}

			logger.setLevel(config.getLevel());

			// 是否当前日志需要输出至文件,则先检查日志文件存放目录是否存在
			if (config.isLog2File() || config.isErr2File()
					|| config.isOut2File()) {
				File file = new File(config.getLogDir());
				if (!file.isDirectory()) {
					file.mkdirs();
				}
			}
			// 设置日志文件Handler
			if (config.isLog2File()) {
				logger.addHandler(config.getLogFileHandler());
			}

			// 设置控制台日志Handler
			if (config.isLog2console()) {
				logger.addHandler(config.getConsoleHandler());
			}
			// 设置运行时异常的Handler
			if (config.isErr2File()) {
				logger.addHandler(config.getErrorFileHandler());
			}

			// 设置System.out的Handler
			if (config.isOut2File()) {
				logger.addHandler(config.getOutFileHandler());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		this.config = config;
	}

	public void log(Throwable thrown) {

		log(Level.WARNING, thrown.getMessage(), thrown);
	}

	public void log(Level level, String msg) {

		log(level, msg, (Throwable) null);
	}

	public void log(Level level, String msg, byte[] data) {
		log(level, msg + "\r\n" + StringUtils.toHexString(data),
				(Throwable) null);
	}

	public void log(Level level, String msg, Throwable thrown) {
		if (!enabled) {
			return;
		}
		if (config == null) {
			synchronized (this) {
				if (config == null) {
					setInnerLoggerCfg();
				}
			}
		}
		LogRecord record = new LogRecord(level, null);
		record.setMessage(msg);
		record.setThrown(thrown);
		logger.log(record);
	}

	/**
	 * 内置一个日至系统配置
	 */
	private void setInnerLoggerCfg() {
		LoggerConfig cfg = new LoggerConfig();
		cfg.setLog2console(true);
		cfg.setLevel(Level.ALL);
		init(cfg);
	}

	public void close() {
		enabled = false;
	}
}
