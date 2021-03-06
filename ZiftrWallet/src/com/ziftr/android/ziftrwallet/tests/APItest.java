/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.tests;

import android.test.ActivityInstrumentationTestCase2;

import com.ziftr.android.ziftrwallet.ZWMainFragmentActivity;
import com.ziftr.android.ziftrwallet.network.ZWApi;
import com.ziftr.android.ziftrwallet.network.ZiftrNetRequest;

public class APItest extends ActivityInstrumentationTestCase2<ZWMainFragmentActivity>{

	public APItest() {
		super(ZWMainFragmentActivity.class);
	}
	
	public void setUp() throws Exception {
		super.setUp();
	}
	
	public void testPreconditions() {
		//server is online and we can successfully communicate with it
		ZiftrNetRequest request = ZWApi.buildBlockchainsRequest();
		String response = request.sendAndWait();
		assertEquals(200, request.getResponseCode());
		assertFalse(response.isEmpty());
	}
	
	public void tearDown() throws Exception{
		super.tearDown();
	}
}
