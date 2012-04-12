package de.teammeet.xmpp;

import org.jivesoftware.smack.packet.PacketExtension;

import android.util.Log;

public class GeolocPacketExtension implements PacketExtension {

	private static String CLASS = GeolocPacketExtension.class.getSimpleName();
	private int mLatitude = 0;
	private int mLongitude = 0;
	private float mAccuracy = 0;
	
	public GeolocPacketExtension(int latitude, int longitude, float accuracy) {
		mLatitude = latitude;
		mLongitude = longitude;
		mAccuracy = accuracy;
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
		return String.format("<x xmlns=\"%s\"><geoloc><lat>%d</lat><long>%d</long><error>%f</error></geoloc></x>", getNamespace(), mLatitude, mLongitude, mAccuracy);
	}
}
