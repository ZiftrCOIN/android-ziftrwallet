package com.ziftr.android.ziftrwallet;

import android.app.Application;
import android.content.pm.ApplicationInfo;

public class OWApplication extends Application {

	/** Describes whether or not this application is debuggable. */
	private static boolean isDebuggable;

	private static OWApplication self;
	
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
		self = this;
		isDebuggable = (0 != 
				(getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
		PRNGFixes.apply();
	}
	
	
	/**
	 * Gets an instance of the currently running application. 
	 * This should rarely be called and could be dangerous.
	 * It's primary purpose is so that classes that only need the application Context
	 * can use lazy initialization.
	 * @return An instance of the Application, which can be used as a Context
	 */
	public static OWApplication getApplication() {
		if(self == null) {
			throw new RuntimeException("Attempting to access Application Context before it's been set. " +
					"Make sure OWApplication class isn't accessing anything statically that might cause a loop back.");
		}
		return self;
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
