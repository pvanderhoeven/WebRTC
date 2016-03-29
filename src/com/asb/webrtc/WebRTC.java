package com.asb.webrtc;

import java.util.ArrayList;

import org.appspot.apprtc.VideoStreamsView;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaConstraints.KeyValuePair;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceConnectionState;
import org.webrtc.PeerConnection.IceGatheringState;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.PeerConnection.SignalingState;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRenderer.I420Frame;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import android.content.Context;
import android.media.AudioManager;

import com.asb.webrtc.Constants.WEBRTC.ROLE;
import com.asb.webrtc.Constants.WEBRTC.STEP;
import com.asb.webrtc.channel.AbstractChannel;
import com.asb.webrtc.channel.AbstractChannel.OnMessagedListener;

public class WebRTC {
	
	private Context mContext;
	
	private AbstractChannel mChannel;
	
	private STEP mCurrentStep;
	
	private ROLE mUserRole;
	private String mLocalUser;
	private String mRemoteUser;
	
	private MediaConstraints mPcConstraints;
	private MediaConstraints mSdpConstraints;
	
	private PeerConnectionFactory mPcFatory;
	private PeerConnection mPc;
	
	private VideoStreamsView mVideoView;
	private MediaStream mLocalStream;
	private MediaStream mRemoteStream;
	
	private SessionDescription mLocalSdp = null;
	private SessionDescription mRemoteSdp = null;
	
	private OnWebRTCStepListener mRtcStepListener;
	private OnSignalingListener mSignalingListener;
	private OnMessageHookingListener mHookingListener;
	private OnPeerConnectionListener mPeerConnectionListener;
	private OnSdpListener mOnSdpListener;
	
	public WebRTC(Context context, ROLE role, String localUser, OnWebRTCStepListener stepListener, VideoStreamsView videoView) {
		mContext = context;
		
		mUserRole = role;
		mLocalUser = localUser;
		mRtcStepListener = stepListener;
		mVideoView = videoView;
		
		mSignalingListener = new OnSignalingListener();
		
		mCurrentStep = STEP.NONE;
	}
	
	public OnMessagedListener initChannel(AbstractChannel channel, OnMessageHookingListener hookingListener) {
		SLog.LOGE("initChannel was called");
		
		mChannel = channel;
		mChannel.setMessagedListener(mSignalingListener);
		mHookingListener = hookingListener;
		
		return mSignalingListener;
	}
	
	public void initWebRtc(boolean useVideo, boolean useAudio, String remoteUser) {
		SLog.LOGE("initWebRtc was called");
		
		mRemoteUser = remoteUser;
		
		mSdpConstraints = new MediaConstraints();
		mSdpConstraints.mandatory.add(new KeyValuePair("OfferToReceiveVideo", useVideo?"true":"false"));
		mSdpConstraints.mandatory.add(new KeyValuePair("OfferToReceiveAudio", useAudio?"true":"false"));
		
		mPcConstraints = new MediaConstraints();
		mPcConstraints.optional.add(new KeyValuePair("DtlsSrtpKeyAgreement", "true"));
		
		AudioManager audioManager = ((AudioManager) mContext.getSystemService(mContext.AUDIO_SERVICE));
		// TODO(fischman): figure out how to do this Right(tm) and remove the
		// suppression.
		@SuppressWarnings("deprecation")
		boolean isWiredHeadsetOn = audioManager.isWiredHeadsetOn();
		audioManager.setMode(isWiredHeadsetOn ? AudioManager.MODE_IN_CALL : AudioManager.MODE_IN_COMMUNICATION);
		audioManager.setSpeakerphoneOn(!isWiredHeadsetOn);
		
		SLog.LOGE("Android global initialized:"+PeerConnectionFactory.initializeAndroidGlobals(mContext, true, true));
		
		mPcFatory = new PeerConnectionFactory();
		
		if(getUserMedia()) {
			changeWebRtcStep(STEP.GUM_SUCCESS);
		}
		else {
			changeWebRtcStep(STEP.GUM_FAILED);
		}
	}
	
	public void startWebRtc(String localUser, ROLE role) {
		mUserRole = role;
		mLocalUser = localUser;
		
		startWebRtc();
	}
	
	public void startWebRtc() {
		//Create Peerconnection
		mPeerConnectionListener = new OnPeerConnectionListener();
		
		ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<PeerConnection.IceServer>();
		iceServers.add(new IceServer("turn:1.237.187.34:3478", "test", "1234"));
		mPc = mPcFatory.createPeerConnection(iceServers, mPcConstraints, mPeerConnectionListener);
		
		//Add localStream to PeerConnection
		mPc.addStream(mLocalStream, new MediaConstraints());
		
		mOnSdpListener =  new OnSdpListener();
		
		//if user is CALLER,
		//create Offer and send to CALLEE
		if(mUserRole == ROLE.CALLER) {
			mPc.createOffer(mOnSdpListener, mSdpConstraints);
		}
	}
	
	public void stopWebRtc(boolean sendBye) {
		stopWebRtc(sendBye, null);
	}
	
	public void stopWebRtc(boolean sendBye, String remoteUser) {
		if(mCurrentStep == STEP.NONE)
			return;
		
		if(mPc != null)
			mPc.close();
		
		if(remoteUser != null) 
			mRemoteUser = remoteUser;
		
		if(mLocalStream != null) {
			//TODO Remove video/audio tracks 
		}
		
		if(sendBye) {
			try {
				JSONObject byeMessage = new JSONObject();
				byeMessage.put("type", "bye");
				
				mChannel.rtcSignaling(byeMessage, mRemoteUser);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		changeWebRtcStep(STEP.DISCONNECTED);
		
		mCurrentStep = STEP.NONE;
	}
	
	private boolean getUserMedia() {
		boolean gumResult = true;
		
		mLocalStream = mPcFatory.createLocalMediaStream("ARDAMS");
		SLog.LOGE(""+0);
		
		//get VideoCapturer
		VideoCapturer capturer = getVideoCapturer();
		SLog.LOGE(""+1);
		
		VideoSource videoSource = mPcFatory.createVideoSource(capturer, new MediaConstraints());
		SLog.LOGE(""+2);
		
		VideoTrack videoTrack = mPcFatory.createVideoTrack("ARDAMSv0", videoSource);
		SLog.LOGE(""+3);
		
		videoTrack.addRenderer(new VideoRenderer(new VideoCallbacks(mVideoView, VideoStreamsView.Endpoint.LOCAL)));
		SLog.LOGE(""+4);
		
		gumResult &= mLocalStream.addTrack(videoTrack);
		SLog.LOGE("Add video track success");
		
		gumResult &= mLocalStream.addTrack(mPcFatory.createAudioTrack("ARDAMSa0", mPcFatory.createAudioSource(new MediaConstraints())));
		SLog.LOGE("Add audio track success");
		
		return gumResult;
	}
	
	private VideoCapturer getVideoCapturer() {
		String[] cameraFacing = { "front", "back" };
		int[] cameraIndex = { 0, 1 };
		int[] cameraOrientation = { 0, 90, 180, 270 };
		for (String facing : cameraFacing) {
			for (int index : cameraIndex) {
				for (int orientation : cameraOrientation) {
					String name = "Camera " + index + ", Facing " + facing + ", Orientation " + orientation;
					VideoCapturer capturer = VideoCapturer.create(name);
					if (capturer != null) {
						return capturer;
					}
				}
			}
		}
		throw new RuntimeException("Failed to open capturer");
	}
	
	private void changeWebRtcStep(STEP step) {
		SLog.LOGE("Current WebRTC Step : "+step.name());
		
		mCurrentStep = step;
		if(mRtcStepListener != null) {
			mRtcStepListener.onStepChanged(step);
		}
	}
	
	private class OnSdpListener implements SdpObserver {

		@Override
		public void onCreateSuccess(SessionDescription sdp) {
			mLocalSdp = sdp;
			
			if(sdp.type == SessionDescription.Type.OFFER) {
				SLog.LOGE("Offer sdp was created");
			}
			else {
				SLog.LOGE("Answer sdp was created");
			}
			
			mPc.setLocalDescription(mOnSdpListener, mLocalSdp);
		}

		@Override
		 * 	
		 *  CALLER				CALLEE
		 * 	c  |				   |	
		 * o,x |				   |	
		 * 	   |				   |
		 * 	s  |------------------>|
		 * o,x |				   |  s	
		 * 	   |				   | x,o	
		 * 	   |				   |	
		 * 	   |				   |  c	
		 * 	   |				   | o,o	
		 * 	   |				   |	
		 * 	s  |<------------------|  s	
		 * o,o |				   | o,o	
		 * 	   |				   |	
		 * 	   |				   |	
		 */
		public void onSetSuccess() {
			if(mLocalSdp != null && mRemoteSdp != null) {
				if(mUserRole == ROLE.CALLEE) {
					SLog.LOGE("Set local sdp set success");
					
					JSONObject answerJson = new JSONObject();
					try {
						answerJson.put("type", mLocalSdp.type.canonicalForm());
						answerJson.put("sdp", mLocalSdp.description);
						
						mChannel.rtcSignaling(answerJson, mRemoteUser);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else {
					SLog.LOGE("Set remote sdp set success");
				}
			}
			else {
				if(mUserRole == ROLE.CALLER) {
					SLog.LOGE("Set local sdp set success");
					
					JSONObject answerJson = new JSONObject();
					try {
						answerJson.put("type", mLocalSdp.type.canonicalForm());
						answerJson.put("sdp", mLocalSdp.description);
						
						mChannel.rtcSignaling(answerJson, mRemoteUser);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else {
					SLog.LOGE("Set remote sdp set success");
					
					mPc.createAnswer(mOnSdpListener, mSdpConstraints);
				}
			}
		}
		
		@Override
		public void onCreateFailure(String error) {
			SLog.LOGE(error);
		}
		
		@Override
		public void onSetFailure(String error) {
			SLog.LOGE(error);
		}
		
	}
	
	private class OnPeerConnectionListener implements PeerConnection.Observer {

		@Override
		public void onAddStream(final MediaStream stream) {
			(new Thread(new Runnable() {

				@Override
				public void run() {
					stream.videoTracks.get(0).addRenderer(new VideoRenderer(new VideoCallbacks(mVideoView, VideoStreamsView.Endpoint.REMOTE)));
				}
				
			})).start();
		}

		@Override
		public void onDataChannel(DataChannel arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onError() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onIceCandidate(IceCandidate candidate) {
			//Send ICE candidate to remote peer
			JSONObject iceCandidate = new JSONObject();
			try {
				iceCandidate.put("type", "candidate");
				iceCandidate.put("label", candidate.sdpMLineIndex);
				iceCandidate.put("id", candidate.sdpMid);
				iceCandidate.put("candidate", candidate.sdp);
				
				mChannel.rtcSignaling(iceCandidate, mRemoteUser);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onIceConnectionChange(IceConnectionState arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onIceGatheringChange(IceGatheringState arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRemoveStream(MediaStream arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRenegotiationNeeded() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSignalingChange(SignalingState arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public class OnSignalingListener implements OnMessagedListener {

		@Override
		public void onMessage(String message) {
			try {
				JSONObject jsonMsg = new JSONObject(message);
				
				String msgType = jsonMsg.getString("TYPE");
				if(msgType.equals("RTC")) {
					JSONObject signalingMsg = new JSONObject(jsonMsg.getString("MESSAGE"));
					
					String signalingType = signalingMsg.getString("type");
					
					//SDP Type == OFFER
					if(signalingType.equals("offer")) {
						mRemoteSdp = new SessionDescription(SessionDescription.Type.OFFER, signalingMsg.getString("sdp"));
						mPc.setRemoteDescription(mOnSdpListener, mRemoteSdp);
					}
					
					//SDP Type == ANSWER
					else if(signalingType.equals("answer")) {
						mRemoteSdp = new SessionDescription(SessionDescription.Type.ANSWER, signalingMsg.getString("sdp"));
						mPc.setRemoteDescription(mOnSdpListener, mRemoteSdp);
					}
					
					//SDP Type == CANDIDATE
					else if(signalingType.equals("candidate")) {
						mPc.addIceCandidate(new IceCandidate(
								signalingMsg.getString("id"), 
								signalingMsg.getInt("label"), 
								signalingMsg.getString("candidate")
						));
					}
					
					//SDP Type == BYE
					else if(signalingType.equals("bye")) {
						stopWebRtc(false);
					}
				}
				else {
					if(mHookingListener != null) {
						mHookingListener.onMessaged(message);
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	private class VideoCallbacks implements VideoRenderer.Callbacks {
		private final VideoStreamsView view;
		private final VideoStreamsView.Endpoint stream;

		public VideoCallbacks(VideoStreamsView view, VideoStreamsView.Endpoint stream) {
			this.view = view;
			this.stream = stream;
		}

		@Override
		public void setSize(final int width, final int height) {
			view.queueEvent(new Runnable() {
				public void run() {
					view.setSize(stream, width, height);
				}
			});
		}

		@Override
		public void renderFrame(I420Frame frame) {
			view.queueFrame(stream, frame);
		}
	}
	
	public interface OnWebRTCStepListener {
		public abstract void onStepChanged(STEP step);
	}
	
	public interface OnMessageHookingListener {
		public abstract void onMessaged(String message);
	}
}
