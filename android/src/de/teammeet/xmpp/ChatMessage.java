package de.teammeet.xmpp;

public class ChatMessage {

	private String mFrom = null;
	private String mTo = null;
	private long mTimestamp = 0;
	private String mMessage = null;

	public ChatMessage(String from, String to, long timestamp, String message) {
		mFrom = from;
		mTo = to;
		mTimestamp = timestamp;
		mMessage = message;
	}

	public String getFrom() {
		return mFrom;
	}

	public String getTo() {
		return mTo;
	}

	public long getTimestamp() {
		return mTimestamp;
	}

	public String getMessage() {
		return mMessage;
	}
}
