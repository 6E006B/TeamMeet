package de.teammeet.services.xmpp;

import org.jivesoftware.smack.packet.PacketExtension;

import android.util.Log;

public class GeolocPacketExtension implements PacketExtension {

	private static final String CLASS = GeolocPacketExtension.class.getSimpleName();

	private int mLongitude = 0;
	private int mLatitude = 0;
	private int mAccuracy = 0;

	public static final String NAMESPACE = "https://teammeet.de/teammeet_geoloc.ns";
	public static final String GEOLOC = "geoloc";
	public static final String LON = "lon";
	public static final String LAT = "lat";
	public static final String ACCURACY = "err";

	public GeolocPacketExtension(int longitude, int latitude, int accuracy) {
		mLongitude = longitude;
		mLatitude = latitude;
		mAccuracy = accuracy;
	}

	@Override
	public String getElementName() {
		return "geoloc";
	}

	@Override
	public String getNamespace() {
		Log.d(CLASS, "getNamespace() called");
		return NAMESPACE;
	}

	@Override
	public String toXML() {
		return String.format("<x xmlns=\"%s\">" +
						     "<%s>" +
				             "<%s>%d</%s>" +
						     "<%s>%d</%s>" +
				             "<%s>%d</%s>" +
						     "</%s>" +
				             "</x>",
				             getNamespace(),
				             GEOLOC,
				             LON, mLongitude, LON,
				             LAT, mLatitude, LAT,
				             ACCURACY, mAccuracy, ACCURACY,
				             GEOLOC);
	}	

	public int getLatitude() {
		return mLatitude;
	}

	public int getLongitude() {
		return mLongitude;
	}

	public int getAccuracy() {
		return mAccuracy;
	}
}
