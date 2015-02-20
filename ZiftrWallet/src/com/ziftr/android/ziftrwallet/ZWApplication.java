package com.ziftr.android.ziftrwallet;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.ziftr.android.ziftrwallet.util.ZiftrTypefaceUtil;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWApplication extends Application {

	/** Describes whether or not this application is debuggable. */
	private static boolean isDebuggable;

	private static ZWApplication self;
	private static int versionCode = 0;
	private static String versionName = "0.0.1";
	
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
	    ZiftrTypefaceUtil.overrideFont(getApplicationContext());
		isDebuggable = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
		
		ZiftrUtils.createTrulySecureRandom(); //just call to initialze fixes here ahead of time
		
		try {
			versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
			
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} 
    	catch (NameNotFoundException e) {
    		Log.e("ZiftrWallet", "Couldn't get version code from manifest.");
		}
	}
	
	
	/**
	 * Gets an instance of the currently running application. 
	 * This should rarely be called and could be dangerous.
	 * It's primary purpose is so that classes that only need the application Context
	 * can use lazy initialization.
	 * @return An instance of the Application, which can be used as a Context
	 */
	public static ZWApplication getApplication() throws NullPointerException {
		if(self == null) {
			throw new NullPointerException("Attempting to access Application Context before it's been set. " +
					"Make sure ZWApplication class isn't accessing anything statically that might cause a loop back.");
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
	
	
	public static int getVersionCode() {
		return versionCode;
	}
	
	
	public static String getVersionName() {
		return versionName;
	}

	
	
}


