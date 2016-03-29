package com.asb.webrtc;

import android.util.Log;

public class SLog {
	
	private static final boolean DEBUG = true;

	private static final String TAG = "jphong";
	
	public static void LOGE(String log) {
		if(DEBUG) {
			Log.e(TAG, log);
		}
	}

}
