package de.teammeet.xmpp;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import android.util.Log;

public class ChatMessageListener implements PacketListener {

	private static final String CLASS = ChatMessageListener.class.getSimpleName();

	private XMPPService mXMPPService = null;

	public ChatMessageListener(XMPPService xmppService) {
		mXMPPService  = xmppService;
	}

	@Override
	public void processPacket(Packet packet) {
		Log.d(CLASS, "ChatMessageListener.processPacket()");
		Message message = (Message)packet;
		final String from = message.getFrom();
		final String to = message.getTo();
		final String body = message.getBody();
		final long timestamp = System.currentTimeMillis();
		ChatMessage chatMessage = new ChatMessage(from, to, timestamp, body);
		Log.d(CLASS, String.format("new message from '%s': %s", from, body));
		mXMPPService.handleNewChatMessage(chatMessage);
	}
}
