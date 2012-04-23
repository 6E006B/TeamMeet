package de.teammeet.xmpp;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import android.util.Log;

import com.google.android.maps.GeoPoint;

import de.teammeet.Mate;

public class RoomMessageListener implements PacketListener {

	private static String CLASS = RoomMessageListener.class.getSimpleName();
	private XMPPService mXMPPService = null;
	private String mGroup = null;

	public RoomMessageListener(XMPPService xmppService, String group) {
		mXMPPService  = xmppService;
		mGroup = group;
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
		final Message message = (Message)packet;
		final String text = message.getBody();
		Log.d(CLASS, "Message body: " + text);
		if (!text.equals("")) {
			final long timestamp = System.currentTimeMillis();
			final GroupChatMessage groupChatMessage = new GroupChatMessage(from, mGroup, timestamp,
			                                                               text);
			mXMPPService.newGroupMessage(groupChatMessage);
		}
		Log.d(CLASS, from + " sent " + xml);
	}
}
