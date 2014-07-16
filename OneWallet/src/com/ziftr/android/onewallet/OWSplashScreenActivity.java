package com.ziftr.android.onewallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;

import com.ziftr.android.onewallet.util.OWUtils;
import com.ziftr.android.onewallet.util.ZLog;

/**
 * This activity is responsible for loading the splash screen and then,
 * one second later, starting the app. It also can load different screens 
 * based on whether or not we are compiling for a tablet.  
 */
public class OWSplashScreenActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	// Here we just load the splash screen and then start the app one
    	// second later.
    	
        super.onCreate(savedInstanceState);

        // First just set view to splash screen
        this.setContentView(R.layout.splash_layout);

        loadAppOneSecondLater();
        
    }
    
    /**
     * The splash screen stays open for one second and then
     * we switch over to the {@link OWMainFragmentActivity} activity.
     */
    private void loadAppOneSecondLater() {
    	OWUtils.runOnNewThread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					ZLog.log("Cut out early....\nCut out early....\nCut out early....");
					e.printStackTrace();
				}

				buildAndInitUi();
		        finish();
			}
		});
    }

    @Override
	public void onBackPressed() {
		// To block back button press during loading screen, we override this
    	// and do not make any call to super.onBackPressed();
	}


	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		// We want the only thing on the screen to be the splash screen,
		// so we disable the options menu.
    	return false;
    }
    
    public void buildAndInitUi() {
	
    	//TODO Currently we only have one version of the UI that should work 
    	// on all devices. This selection code will let us have multiple versions 
    	// of the UI with the same code base if we want. All we will have to do
    	// is change the value of isTablet in flags.xml and recompile for tablet.
    	
		boolean isTablet = getResources().getBoolean(R.bool.isTablet);
        
		if (isTablet) {
    		loadTabletUi();
    	} else {
    		loadFragmentBasedUi();
    	}
	}
	
	
    private void loadNoFragmentUi() {
    	Intent noFragmentIntent = new Intent(OWSplashScreenActivity.this, 
    			OWMainFragmentActivity.class);
    	noFragmentIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(noFragmentIntent);
    }
    
    private void loadFragmentBasedUi() {
    	//TODO Load different UI for devices that can use fragments and action bars
    	loadNoFragmentUi();
    }
    
    private void loadTabletUi() {
    	//TODO Implement different UI for tablets
    	loadNoFragmentUi();
    }
    
}
