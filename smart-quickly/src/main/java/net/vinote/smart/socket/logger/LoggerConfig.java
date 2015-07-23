package net.vinote.smart.socket.logger;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * 
 * 日志工具RunLogger配置参数类
 * 
 * @author 郑俊伟
 * 
 */
public class LoggerConfig {
	private static final int MB = 1024 * 1024;

	/** 日志级别 */
	private Level level;

	/** 日志是否输出至控制台 */
	private boolean log2console;

	/** 日志记录编码方式 */
	private String encoding;

	/** 日志文件限制大小,单位MB */
	private int limit;

	/** 日志文件存放目录 */
	private String logDir;

	/** 当前日志系统名称 */
	private String logName;

	/** 日志文件输出至文件 */
	private boolean log2File;

	/** System.err流输出至文件 */
	private boolean err2File;

	/** System.out流输出至文件 */
	private boolean out2File;

	/** 是否采用同步方式记录日志 */
	private boolean synchro;

	/**
	 * 是否需要将日志输出至文件
	 * 
	 * @return
	 */
	public boolean isLog2File() {
		return log2File;
	}

	public String getLogDir() {
		return (logDir != null && !logDir.endsWith(File.separator)) ? logDir
				+ File.separator : logDir;
	}

	public void setLogDir(String logDir) {
		this.logDir = logDir;
	}

	public String getLogName() {
		return logName;
	}

	public void setLogName(String logName) {
		this.logName = logName;
	}

	public String getEncoding() {
		return encoding == null ? "utf-8" : encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public int getLimit() {
		return limit * MB;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public Level getLevel() {
		return level;
	}

	public void setLevel(Level level) {
		this.level = level;
	}

	public boolean isErr2File() {
		return err2File;
	}

	public void setErr2File(boolean err2File) {
		this.err2File = err2File;
	}

	public boolean isOut2File() {
		return out2File;
	}

	public void setOut2File(boolean out2File) {
		this.out2File = out2File;
	}

	public boolean isSynchro() {
		return synchro;
	}

	public void setSynchro(boolean synchro) {
		this.synchro = synchro;
	}

	public void setLog2File(boolean log2File) {
		this.log2File = log2File;
	}

	public boolean isLog2console() {
		return log2console;
	}

	public void setLog2console(boolean log2console) {
		this.log2console = log2console;
	}

	Handler getLogFileHandler() {
		Handler fh = null;
		try {
			LogFormatter sf = new LogFormatter();
			fh = new FileHandler(getLogDir() + getLogName(), getLimit(), 100,
					true);
			fh.setFormatter(sf);
			fh.setEncoding(getEncoding());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fh;
	}

	/**
	 * 获取运行时异常的Handler;文件输出流设置为System.err,用于捕获异常、错误信息
	 * 
	 * @return
	 */
	Handler getErrorFileHandler() {
		Handler errFh = null;
		try {
			LogFormatter sf = new LogFormatter();
			// 设置运行时异常的Handler
			errFh = new FileHandler(getLogDir() + "error.log", getLimit(), 100,
					true) {
				
				protected synchronized void setOutputStream(OutputStream out)
						throws SecurityException {
					System.setErr(new PrintStream(out, true));
				}
			};
			errFh.setFormatter(sf);
			errFh.setEncoding(getEncoding());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return errFh;
	}

	/**
	 * 文件输出流设置为System.out,并将该输出流设置为FileHandler
	 * 
	 * @return
	 */
	Handler getOutFileHandler() {
		Handler outFh = null;
		try {
			LogFormatter sf = new LogFormatter();
			// 设置运行时异常的Handler
			outFh = new FileHandler(getLogDir() + "out.log", getLimit(), 100,
					true) {
				
				protected synchronized void setOutputStream(OutputStream out)
						throws SecurityException {
					System.setOut(new PrintStream(out, true));
				}
			};
			outFh.setFormatter(sf);
			outFh.setEncoding(getEncoding());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return outFh;
	}

	Handler getConsoleHandler() {
		ConsoleHandler ch = new ConsoleHandler();
		ch.setFormatter(new LogFormatter());
		ch.setLevel(Level.ALL);
		try {
			ch.setEncoding(getEncoding());
			// ch.setEncoding("utf8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ch;
	}
}
