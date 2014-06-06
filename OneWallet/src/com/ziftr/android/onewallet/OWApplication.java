package com.ziftr.android.onewallet;

import android.app.Application;
import android.content.pm.ApplicationInfo;

public class OWApplication extends Application {

	
	private static boolean isDebuggable;
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		//this is the first code called when android app launches (even before statics are loaded
		//as long as no outside classes are referenced here (which would attempt to load their static members),
		//data can be initialized in this method and then be accessed statically by the rest of the app
		
		isDebuggable = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
	}
	
	
	public static boolean isDebuggable() {
		return isDebuggable;
	}

}
