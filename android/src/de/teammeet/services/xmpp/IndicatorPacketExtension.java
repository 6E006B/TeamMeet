package de.teammeet.services.xmpp;

import org.jivesoftware.smack.packet.PacketExtension;

public class IndicatorPacketExtension implements PacketExtension {

	public static final String NAMESPACE = "https://teammeet.de/teammeet_indicator.ns";
	public static final String INDICATOR = "indicator";
	public static final String LON = "lon";
	public static final String LAT = "lat";
	public static final String INFO = "info";

	private int mLatitude = 0;
	private int mLongitude = 0;
	private String mInfo;

	public IndicatorPacketExtension(int latitude, int longitude, String info) {
		mLatitude = latitude;
		mLongitude  = longitude;
		mInfo = info;
	}

	@Override
	public String getElementName() {
		return INDICATOR;
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public String toXML() {
		return String.format("<x xmlns=\"%s\">" +
						     "<%s>" +
				             "<%s>%d</%s>" +
						     "<%s>%d</%s>" +
						     "<%s>%s</%s>" +
						     "</%s>" +
				             "</x>",
				             getNamespace(),
				             INDICATOR,
				             LON, mLongitude, LON,
				             LAT, mLatitude, LAT,
				             INFO, mInfo, INFO,
				             INDICATOR);
	}

	public int getLatitude() {
		return mLatitude;
	}

	public int getLongitude() {
		return mLongitude;
	}

	public String getInfo() {
		return mInfo;
	}
}
