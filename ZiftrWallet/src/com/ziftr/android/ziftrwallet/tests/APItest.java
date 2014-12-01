package com.ziftr.android.ziftrwallet.tests;

import android.test.ActivityInstrumentationTestCase2;

import com.ziftr.android.ziftrwallet.OWMainFragmentActivity;
import com.ziftr.android.ziftrwallet.network.OWApi;
import com.ziftr.android.ziftrwallet.network.ZiftrNetRequest;

public class APItest extends ActivityInstrumentationTestCase2<OWMainFragmentActivity>{

	public APItest() {
		super(OWMainFragmentActivity.class);
	}
	
	public void setUp() throws Exception {
		super.setUp();
	}
	
	public void testPreconditions() {
		//server is online and we can successfully communicate with it
		ZiftrNetRequest request = OWApi.buildGenericApiRequest(true, "blockchains");
		String response = request.sendAndWait();
		assertEquals(200, request.getResponseCode());
		assertFalse(response.isEmpty());
	}
	
	public void tearDown() throws Exception{
		super.tearDown();
	}
}
