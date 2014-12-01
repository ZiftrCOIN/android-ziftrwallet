package com.ziftr.android.ziftrwallet.util;

import java.lang.reflect.Field;

import android.content.Context;
import android.graphics.Typeface;

public class OWTypefaceUtil {
	
	public static void overrideFont(Context context){
        try {
			final Typeface ubuntuLight = Typeface.createFromAsset(context.getAssets(), "fonts/Ubuntu-L.ttf");
			//overide Serif font with UbuntuLight
            final Field defaultFontTypefaceField = Typeface.class.getDeclaredField("SERIF");
            defaultFontTypefaceField.setAccessible(true);
            defaultFontTypefaceField.set(null, ubuntuLight);
        } catch (Exception e) {
        	ZLog.log("Error loading custom font");
        }

	}
}
