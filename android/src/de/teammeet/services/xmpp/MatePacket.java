package de.teammeet.services.xmpp;

public class MatePacket {

	private int mLongitude = 0;
	private int mLatitude = 0;
	private int mAccuracy = 0;

	public MatePacket(int longitude, int latitude, int accuracy) {
		mLongitude = longitude;
		mLatitude = latitude;
		mAccuracy = accuracy;
	}

	public int getLongitude() {
		return mLongitude;
	}

	public int getLatitude() {
		return mLatitude;
	}

	public int getAccuracy() {
		return mAccuracy;
	}

	public String toXML() {
		return String.format("<%s>" +
				             "<%s>%d</%s>" +
						     "<%s>%d</%s>" +
				             "<%s>%d</%s>" +
						     "</%s>",
				             TeamMeetPacketExtension.MATE,
				             TeamMeetPacketExtension.LON, mLongitude, TeamMeetPacketExtension.LON,
				             TeamMeetPacketExtension.LAT, mLatitude, TeamMeetPacketExtension.LAT,
				             TeamMeetPacketExtension.ACCURACY, mAccuracy, TeamMeetPacketExtension.ACCURACY,
				             TeamMeetPacketExtension.MATE);
	}
}