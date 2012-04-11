package de.teammeet.xmpp;

import org.jivesoftware.smack.packet.PacketExtension;

import android.util.Log;

public class GeolocPacketExtension implements PacketExtension {

	private static String CLASS = GeolocPacketExtension.class.getSimpleName();
	private int mLatitude = 0;
	private int mLongitude = 0;
	private int mError = 0;
	
	public GeolocPacketExtension(int latitude, int longitude, int error) {
		mLatitude = latitude;
		mLongitude = longitude;
		mError = error;
	}
	
	@Override
	public String getElementName() {
		return "geoloc";
	}

	@Override
	public String getNamespace() {
		Log.d(CLASS, "getNamespace() called");
		return "https://teammeet.de/geoloc.ns";
	}

	@Override
	public String toXML() {
		return String.format("<x xmlns=\"%s\"><geoloc><lat>%d</lat><long>%d</long><error>%s</error></geoloc></x>", getNamespace(), mLatitude, mLongitude, mError);
	}
}
