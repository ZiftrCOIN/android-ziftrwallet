/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import android.util.Log;

import com.ziftr.android.ziftrwallet.ZWApplication;

public abstract class ZLog {
	
	public static final int DEFAULT_LOGLEVEL = 0;
	
	public static final int FILE_LOGGER = 1;
	public static final int ANDROID_LOGGER = 2;
	public static final int NOOP_LOGGER = 3;
	private static final String LOG_FILE_NAME = "Zlog.txt";
	
	private static ZLog logger = initLogger();
	
	public static void setLogger(int loggerType){
		switch(loggerType){
			case FILE_LOGGER:
				logger = new FileLogger();
				break;
			case ANDROID_LOGGER:
				logger = new AndroidLogger();
				break;
			case NOOP_LOGGER:
				logger = new NoOpLogger();
				break;
		}
	}
	
	private static ZLog initLogger() {
		if(ZWApplication.isDebuggable() ) { 
			
			return new AndroidLogger();
		}
		
		return new NoOpLogger();
	}
	
	
	public static void log(String tag, int logLevel, Object... messages) {
		logger.doLog(tag, logLevel, messages);
	}
	
	public static void log(Object... messages) {
		logger.doLog(messages);
	}
	
	public static void forceFullLog(String bigString) {
		
		if(bigString == null) {
			bigString = "null";
		}
		
		int charCount = 0;
		while(charCount < bigString.length()) {
			
			if ( (charCount + 3000) > bigString.length()) {
				log(bigString.substring(charCount));
			}
			else {
				log(bigString.substring(charCount, charCount +3000));
			}
			
			charCount += 3000;
		}
	}
	
	
	private static String getCurrentClassName(int stackDepth) {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		
		if (elements.length > stackDepth) {
			StackTraceElement element = elements[stackDepth];
			String className = element.getClassName();
			if (className.contains(".")) {
				className = className.substring(className.lastIndexOf(".")+1);
			}
			return className;
		}
		
		return "Ziftr Class";
		
	}
	
	
	
	protected abstract void doLog(String tag, int logLevel, Object... messages);
	protected abstract void doLog(Object... messages);
	protected int logLevel = 10;
	
	
	//@SuppressWarnings("unused")
	private static class AndroidLogger extends ZLog {

		@Override
		protected void doLog(String tag, int logLevel, Object... messages) {
			
			if (logLevel <= this.logLevel) {
				
				StringBuilder logMessage = new StringBuilder();
				for (Object msg : messages) {
					if (msg != null) {
						if (msg instanceof Throwable) {
							Throwable e = (Throwable)msg;
							logMessage.append(Log.getStackTraceString(e));
						}
						else {
							logMessage.append(msg.toString());
						}
					}
				}
				
				if (logLevel <= 0) {
					Log.e(tag, logMessage.toString());
				}
				else if (logLevel <= 1) {
					Log.w(tag, logMessage.toString());
				}
				else if (logLevel <= 2) {
					Log.d(tag, logMessage.toString());
				}
				else if (logLevel <= 3) {
					Log.i(tag, logMessage.toString());
				}
				else {
					Log.v(tag, logMessage.toString());
				}
				

			}
			
		}

		@Override
		protected void doLog(Object... messages) {
			this.doLog(getCurrentClassName(6), DEFAULT_LOGLEVEL, messages);
			
		}
		
	}
	
	private static class NoOpLogger extends ZLog {

		@Override
		protected void doLog(String tag, int logLevel, Object... messages) {
			//noop
		}

		@Override
		protected void doLog(Object... messages) {
			//noop
		}
		
	}
	
	private static class FileLogger extends ZLog {

		@Override
		protected void doLog(String tag, int logLevel, Object... messages) {
			if (logLevel <= this.logLevel) {
				StringBuilder logMessage = new StringBuilder();
				for (Object msg : messages) {
					if (msg != null) {
						if (msg instanceof Throwable) {
							Throwable e = (Throwable)msg;
							logMessage.append(Log.getStackTraceString(e));
						}
						else {
							logMessage.append(msg.toString());
						}
					}
				}
				Log.e(tag, logMessage.toString());
				try {
					File dir = ZWApplication.getApplication().getExternalFilesDir(null);
					if (!dir.exists()){
						dir.mkdirs();
					}
					File logfile = new File(dir, LOG_FILE_NAME);
					FileOutputStream f;
					try {
						f = new FileOutputStream(logfile, true);
						OutputStreamWriter writer = new OutputStreamWriter(f);
						writer.write(logMessage.toString() + "\n");
						writer.flush();
						writer.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (NullPointerException e) {
					Log.e(tag, "null application, shouldn't happen: " + e);
					return;
				}
			}
		}

		@Override
		protected void doLog(Object... messages) {
			this.doLog(getCurrentClassName(6), DEFAULT_LOGLEVEL, messages);
		}
		
	}
	

}
