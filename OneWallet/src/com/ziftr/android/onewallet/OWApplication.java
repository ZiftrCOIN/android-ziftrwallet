package com.ziftr.android.onewallet;

import android.app.Application;
import android.content.pm.ApplicationInfo;

public class OWApplication extends Application {

	/** Describes whether or not this application is debuggable. */
	private static boolean isDebuggable;

	/**
	 * This is the first code called when android app launches (even 
	 * before statics are loaded. as long as no outside classes are 
	 * referenced here (which would attempt to load their static members),
	 * data can be initialized in this method and then be accessed 
	 * statically by the rest of the app.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		isDebuggable = (0 != 
				(getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
		PRNGFixes.apply();
	}

	/**
	 * Gives a boolean which describes whether or not this application 
	 * is debuggable. 
	 * 
	 * @return - a boolean which describes whether or not this application 
	 * is debuggable.
	 */
	public static boolean isDebuggable() {
		return isDebuggable;
	}

}
