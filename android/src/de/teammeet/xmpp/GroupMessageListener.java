package de.teammeet.xmpp;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;

import android.util.Log;

public class GroupMessageListener implements PacketListener {

	private static String	CLASS	= GroupMessageListener.class.getSimpleName();

	public GroupMessageListener() {
	}

	@Override
	public void processPacket(Packet packet) {
		String from = packet.getFrom();
		String xml = packet.toXML();
		GeolocPacketExtension geoloc = (GeolocPacketExtension) packet
				.getExtension(GeolocPacketExtension.NAMESPACE);
		if (geoloc != null) {
			Log.d(CLASS,
					"geoloc - latitude: " + geoloc.getLatitude() + " longitude: " + geoloc.getLongitude());
		}
		Log.d(CLASS, from + " sent " + xml);
	}
}
