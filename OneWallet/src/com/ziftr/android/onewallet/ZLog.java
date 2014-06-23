package com.ziftr.android.onewallet;

import android.util.Log;

public abstract class ZLog {
	
	public static final int DEFAULT_LOGLEVEL = 0;
	
	//private static ZLog logger = new AndroidLogger(); //logging on
	//private static ZLog logger = new NoOpLogger(); //logging off
	
	
	private static final ZLog logger = initLogger();
	
	private static ZLog initLogger() {
		if(OWApplication.isDebuggable()) {
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
		
		int charCount = 0;
		while(charCount < bigString.length()) {
			
			if( (charCount + 3000) > bigString.length()) {
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
		
		if(elements.length > stackDepth) {
			StackTraceElement element = elements[stackDepth];
			String className = element.getClassName();
			if(className.contains(".")) {
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
			
			if(logLevel <= this.logLevel) {
				
				StringBuilder logMessage = new StringBuilder();
				for(Object msg : messages) {
					if(msg != null) {
						if(msg instanceof Exception) {
							Exception e = (Exception)msg;
							logMessage.append(Log.getStackTraceString(e));
						}
						else {
							logMessage.append(msg.toString());
						}
					}
				}
				
				if(logLevel <= 0) {
					Log.e(tag, logMessage.toString());
				}
				else if(logLevel <= 1) {
					Log.w(tag, logMessage.toString());
				}
				else if(logLevel <= 2) {
					Log.d(tag, logMessage.toString());
				}
				else if(logLevel <= 3) {
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
	

}
