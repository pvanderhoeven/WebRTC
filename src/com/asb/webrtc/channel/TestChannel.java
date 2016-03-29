package com.asb.webrtc.channel;

import java.net.URI;

import org.json.JSONException;
import org.json.JSONObject;

import com.asb.webrtc.Constants.CHANNEL;

public class TestChannel extends AbstractChannel {
	
	private int myId;
	
	public TestChannel(URI channelServer) {
		super(channelServer);
	}
	
	public void enter() {
		JSONObject enterRsps = new JSONObject();
		try {
			enterRsps.put("type", "enter");
			send(enterRsps.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void exit() {
		JSONObject exitRqst = new JSONObject();
		try {
			exitRqst.put("type", "exit");
			exitRqst.put("id", myId);
			send(exitRqst.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onMessage(String message) {
		super.onMessage(message);
		
		try {
			JSONObject response = new JSONObject(message);
			
			String type = response.getString("type");
			
			if(type.equals("enter")) {
				myId = response.getInt("id");
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

}
