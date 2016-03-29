package com.asb.webrtc;

import java.net.URI;
import java.net.URISyntaxException;

import org.appspot.apprtc.VideoStreamsView;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.asb.webrtc.Constants.CHANNEL;
import com.asb.webrtc.Constants.WEBRTC.ROLE;
import com.asb.webrtc.Constants.WEBRTC.STEP;
import com.asb.webrtc.channel.AbstractChannel;
import com.asb.webrtc.channel.TestChannel;

public class MainActivity extends Activity {
	
	VideoStreamsView vsv;
	
	TestChannel mChannel;
	WebRTC mWebRTC = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		initLayout();
	}
	
	private void initLayout() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		Point displaySize = new Point();
		getWindowManager().getDefaultDisplay().getSize(displaySize);
		vsv = new VideoStreamsView(this, displaySize);
		
		setContentView(vsv);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
		case R.id.action_init:
			initRTC();
			break;
		case R.id.action_connect:
			if(mWebRTC != null)
				mWebRTC.startWebRtc();
			break;
		}
		
		return true;
	}
	
	private void initRTC() {
		if(mChannel == null) {
			try {
				URI server = new URI(CHANNEL.SERVER);
				mChannel = new TestChannel(server);
				mChannel.setChannelListener(new AbstractChannel.OnChannelListener() {
					
					@Override
					public void onOpen() {
						// TODO Auto-generated method stub
						mChannel.enter();
					}
					
					@Override
					public void onError() {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void onClose() {
						// TODO Auto-generated method stub
						
					}
				});
				mChannel.setMessagedListener(new AbstractChannel.OnMessagedListener() {
					
					@Override
					public void onMessage(String message) {
						try {
							JSONObject response = new JSONObject(message);
							
							String type = response.getString("type");
							
							if(type.equals("enter")) {
								int id = response.getInt("id");	
								
								mWebRTC = new WebRTC(getApplicationContext(), id==0?ROLE.CALLER:ROLE.CALLEE, ""+id, new WebRTC.OnWebRTCStepListener() {
									
									@Override
									public void onStepChanged(STEP step) {
										// TODO Auto-generated method stub
										
									}
									
								}, vsv);
								
								mWebRTC.initChannel(mChannel, new WebRTC.OnMessageHookingListener() {
									
									@Override
									public void onMessaged(String message) {
										
									}
								});
								
								mWebRTC.initWebRtc(true, true, id==0?"1":"0");
							}
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
				});
				
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void startRTC() {
		
	}

}
