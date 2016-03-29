package com.asb.webrtc;

public class Constants {
	
	public static class CHANNEL {
		public static final String SERVER = "ws://localhost:8070";
	}
	
	public static class WEBRTC {
		public static final String SERVER = "";
		
		public static final int CALLER = 10;
		public static final int CALLEE = 11;
		
		public static enum ROLE {
			NONE(10),
			CALLER(11),
			CALLEE(12);
			
			private int value;
			
			private ROLE(int value) {
				this.value = value;
			}
			
			public int value() {
				return this.value;
			}
		};
		
		public static enum STEP {
			NONE(100),
			INIT(101),
			GUM_SUCCESS(102),
			GUM_FAILED(103),
			SIGNALING(104),
			CONNECTED(105),
			DISCONNECTED(106);
			
			private int value;
			
			private STEP(int value) {
				this.value = value;
			}
			
			public int value() {
				return this.value;
			}
		};
	}
	
}
