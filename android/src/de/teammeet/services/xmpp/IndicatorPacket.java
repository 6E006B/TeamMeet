package de.teammeet.services.xmpp;

public class IndicatorPacket {

	private int mLongitude = 0;
	private int mLatitude = 0;
	private String mInfo;

	public IndicatorPacket(int longitude, int latitude, String info) {
		mLongitude = longitude;
		mLatitude = latitude;
		mInfo = info;
	}

	public int getLongitude() {
		return mLongitude;
	}

	public int getLatitude() {
		return mLatitude;
	}

	public String getInfo() {
		return mInfo;
	}

	public String toXML() {
		return String.format("<%s>" +
				             "<%s>%d</%s>" +
						     "<%s>%d</%s>" +
						     "<%s>%s</%s>" +
						     "</%s>",
				             TeamMeetPacketExtension.INDICATOR,
				             TeamMeetPacketExtension.LON, mLongitude, TeamMeetPacketExtension.LON,
				             TeamMeetPacketExtension.LAT, mLatitude, TeamMeetPacketExtension.LAT,
				             TeamMeetPacketExtension.INFO, mInfo, TeamMeetPacketExtension.INFO,
				             TeamMeetPacketExtension.INDICATOR);
	}
}
