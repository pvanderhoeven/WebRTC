package com.asb.webrtc.channel;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

public class AbstractChannel extends WebSocketClient {
	
	protected OnChannelListener mChannelListener;
	protected OnMessagedListener mMessagedListener;
	
	public AbstractChannel(URI serverURI) {
		super(serverURI, new Draft_17());
	}
	
	public void setChannelListener(OnChannelListener channelListener) {
		mChannelListener = channelListener;
	}
	
	public void setMessagedListener(OnMessagedListener messagedListener) {
		mMessagedListener = messagedListener;
	}
	
	public void rtcSignaling(JSONObject message, String sender) {
		JSONObject signalingMsg = new JSONObject();
		
		try {
			signalingMsg.put("type", "rtc");
			signalingMsg.put("rtc", message.toString());
			signalingMsg.put("recevier", Integer.parseInt(sender));
			
			send(signalingMsg.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void onClose(int arg0, String arg1, boolean arg2) {
		// TODO Auto-generated method stub
		if(mChannelListener != null) {
			mChannelListener.onClose();
		}
	}

	@Override
	public void onError(Exception arg0) {
		if(mChannelListener != null) {
			mChannelListener.onError();
		}
	}

	@Override
	public void onMessage(String message) {
		if(mMessagedListener != null) {
			mMessagedListener.onMessage(message);
		}
	}

	@Override
	public void onOpen(ServerHandshake arg0) {
		if(mChannelListener != null) {
			mChannelListener.onOpen();
		}
	}
	
	/**
	 * Listener Methods
	 * 
	 * @author jphofasb
	 */
	public interface OnChannelListener {
		public abstract void onOpen();
		public abstract void onError();
		public abstract void onClose();
	}
	
	public interface OnMessagedListener {
		public abstract void onMessage(String message);
	}
	
}
