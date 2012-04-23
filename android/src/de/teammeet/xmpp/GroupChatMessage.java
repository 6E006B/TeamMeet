package de.teammeet.xmpp;

public class GroupChatMessage {

	private String mFrom = null;
	private String mGroup = null;
	private long mTimestamp = 0;
	private String mMessage = null;
	
	public GroupChatMessage(String from, String group, long timestamp, String message) {
		mFrom = from;
		mGroup = group;
		mTimestamp = timestamp;
		mMessage = message;
	}

	public String getFrom() {
		return mFrom;
	}

	public String getGroup() {
		return mGroup;
	}

	public long getTimestamp() {
		return mTimestamp;
	}

	public String getMessage() {
		return mMessage;
	}
}
