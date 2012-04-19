package de.teammeet.xmpp;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;

import android.util.Log;

import com.google.android.maps.GeoPoint;

import de.teammeet.Mate;

public class RoomMessageListener implements PacketListener {

	private static String CLASS = RoomMessageListener.class.getSimpleName();
	private XMPPService mXMPPService = null;

	public RoomMessageListener(XMPPService xmppService) {
		mXMPPService  = xmppService;
	}

	@Override
	public void processPacket(Packet packet) {
		String from = packet.getFrom();
		String xml = packet.toXML();
		GeolocPacketExtension geoloc = (GeolocPacketExtension) packet
				.getExtension(GeolocPacketExtension.NAMESPACE);
		if (geoloc != null) {
			int lon = geoloc.getLongitude();
			int lat = geoloc.getLatitude();
			GeoPoint location = new GeoPoint(lon, lat);
			float accuracy = geoloc.getAccuracy();
			Log.d(CLASS, "geoloc - longitude: " + lon + " latitude: " + lat  + " accuracy: " + 
						 accuracy);
			Mate mate = new Mate(from, location, accuracy);
			mXMPPService.updateMate(mate);
		}
		Log.d(CLASS, from + " sent " + xml);
	}
}
