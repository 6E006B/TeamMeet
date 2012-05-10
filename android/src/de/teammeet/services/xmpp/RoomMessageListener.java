package de.teammeet.services.xmpp;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import android.util.Log;

import com.google.android.maps.GeoPoint;

import de.teammeet.activities.teams.Mate;

public class RoomMessageListener implements PacketListener {

	private static String CLASS = RoomMessageListener.class.getSimpleName();
	private XMPPService mXMPPService = null;
	private String mGroup = null;

	public RoomMessageListener(XMPPService xmppService, String group) {
		Log.d(CLASS, "new RoomMessageListener for " + group);
		mXMPPService  = xmppService;
		mGroup = group;
	}

	@Override
	public void processPacket(Packet packet) {
		Log.d(CLASS, "RoomMessageListener.processPacket() in " + mGroup);
		String from = packet.getFrom();
		String xml = packet.toXML();
		GeolocPacketExtension geoloc = (GeolocPacketExtension) packet
				.getExtension(GeolocPacketExtension.NAMESPACE);
		if (geoloc != null) {
			int lon = geoloc.getLongitude();
			int lat = geoloc.getLatitude();
			GeoPoint location = new GeoPoint(lat, lon);
			int accuracy = geoloc.getAccuracy();
			Log.d(CLASS,
			      String.format("received location update from '%s' - lon: %d lat: %d acc: %d",
			                    from, lon, lat, accuracy));
			Mate mate = new Mate(from, location, accuracy);
			mXMPPService.updateMate(mate);
		} else {
			Log.d(CLASS, "packet did not contain geoloc extension.");
		}
		Log.d(CLASS, "now checking for indicator");
		IndicatorPacketExtension indicator = (IndicatorPacketExtension) packet
				.getExtension(IndicatorPacketExtension.NAMESPACE);
		if (indicator != null) {
			Log.d(CLASS, "found indicator packet");
			int lon = indicator.getLongitude();
			int lat = indicator.getLatitude();
			GeoPoint location = new GeoPoint(lat, lon);
			String info = indicator.getInfo();
			Log.d(CLASS, "received indicator from '" + from + "' - lon: " + lon + " lat: " + lat +
			      " info: " + info);
		} else {
			Log.d(CLASS, "packet did not contain indicator extension.");
		}
		final Message message = (Message)packet;
		final String text = message.getBody();
		Log.d(CLASS, "Message body: " + text);
		if (!text.equals("") && !message.getFrom().equals(mGroup)) {
			final long timestamp = System.currentTimeMillis();
			final ChatMessage chatMessage = new ChatMessage(from, mGroup, timestamp,
			                                                               text);
			mXMPPService.newGroupMessage(chatMessage);
		}
		Log.d(CLASS, from + " sent " + xml);
	}
}
