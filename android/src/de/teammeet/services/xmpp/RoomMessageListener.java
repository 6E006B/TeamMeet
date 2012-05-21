package de.teammeet.services.xmpp;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import android.util.Log;

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
		TeamMeetPacketExtension teamMeetPacket = (TeamMeetPacketExtension) packet
				.getExtension(TeamMeetPacketExtension.NAMESPACE);
		if (teamMeetPacket != null) {
			if (teamMeetPacket.hasMatePacket()) {
				MatePacket matePacket = teamMeetPacket.getMatePacket();
				int lon = matePacket.getLongitude();
				int lat = matePacket.getLatitude();
				int accuracy = matePacket.getAccuracy();
				Log.d(CLASS,
				      String.format("received location update from '%s' - lon: %d lat: %d acc: %d",
				                    from, lon, lat, accuracy));
				mXMPPService.updateMate(from, lon, lat, accuracy, mGroup);
			} else {
				Log.d(CLASS, "packet did not contain geoloc extension.");
			}
			if (teamMeetPacket.hasIndicatorPacket()) {
				IndicatorPacket indicatorPacket = teamMeetPacket.getIndicatorPacket();
				Log.d(CLASS, "received indicator from '" + from +
				             "' - lon: " + indicatorPacket.getLongitude() +
				             " lat: " + indicatorPacket.getLatitude() +
				             " info: " + indicatorPacket.getInfo());
				mXMPPService.broadcastIndicator(indicatorPacket, mGroup);
			} else {
				Log.d(CLASS, "packet did not contain indicator extension.");
			}
		}
		final Message message = (Message)packet;
		final String text = message.getBody();
		Log.d(CLASS, "Message body: " + text);
		if (!text.equals("") && !message.getFrom().equals(mGroup)) {
			final long timestamp = System.currentTimeMillis();
			final ChatMessage chatMessage = new ChatMessage(from, mGroup, timestamp, text);
			mXMPPService.newGroupMessage(chatMessage);
		}
		Log.d(CLASS, from + " sent " + xml);
	}
}
