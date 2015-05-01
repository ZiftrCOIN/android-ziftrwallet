/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */

package com.ziftr.android.ziftrwallet.network;

import java.util.ArrayList;

public class ZiftrParamList {

	private ArrayList<String[]> params;
	
	public ZiftrParamList () {
		params = new ArrayList<String[]>();
	}
	
	public ZiftrParamList(int capacity) {
		params = new ArrayList<String[]>(capacity);
	}

	public void add(String name, String value) {
		String[] paramPair = new String[]{name, value};
		params.add(paramPair);
	}
	
	public String getName(int index) {
		return params.get(index)[0];
	}
	
	public String getValue(int index) {
		return params.get(index)[1];
	}
	
	public int size() {
		return params.size();
	}
}
