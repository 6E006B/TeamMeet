package de.teammeet.services.xmpp;

public class IndicatorPacket {

	private int mLongitude = 0;
	private int mLatitude = 0;
	private String mInfo;
	private boolean mRemove;

	public IndicatorPacket(int longitude, int latitude, String info) {
		this(longitude, latitude, info, false);
	}

	public IndicatorPacket(int longitude, int latitude, String info, boolean remove) {
		mLongitude = longitude;
		mLatitude = latitude;
		mInfo = info;
		mRemove = remove;
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

	public boolean isRemove() {
		return mRemove;
	}

	public String toXML() {
		String removeXML = "";
		if (mRemove) {
			removeXML = String.format("<%s />", TeamMeetPacketExtension.REMOVE);
		}
		return String.format("<%s>" +
				             "<%s>%d</%s>" +
						     "<%s>%d</%s>" +
						     "<%s>%s</%s>" +
						     "%s" +
						     "</%s>",
				             TeamMeetPacketExtension.INDICATOR,
				             TeamMeetPacketExtension.LON, mLongitude, TeamMeetPacketExtension.LON,
				             TeamMeetPacketExtension.LAT, mLatitude, TeamMeetPacketExtension.LAT,
				             TeamMeetPacketExtension.INFO, mInfo, TeamMeetPacketExtension.INFO,
				             removeXML,
				             TeamMeetPacketExtension.INDICATOR);
	}
}
