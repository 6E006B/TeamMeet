package de.teammeet.xmpp;

import org.jivesoftware.smack.packet.PacketExtension;

public class IndicatorPacketExtension implements PacketExtension {

	private int mLatitude = 0;
	private int mLongitude = 0;

	public IndicatorPacketExtension(int latitude, int longitude) {
		mLatitude = latitude;
		mLongitude  = longitude;
	}
	
	@Override
	public String getElementName() {
		return "indicator";
	}

	@Override
	public String getNamespace() {
		return "https://teammeet.de/teammeet.ns";
	}

	@Override
	public String toXML() {
		return String.format("<x xmlns=\"%s\"><indicator><lat>%d</lat><long>%d</long></indicator></x>", getNamespace(), mLatitude, mLongitude);
	}

}
