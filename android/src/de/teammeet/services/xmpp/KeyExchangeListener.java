package de.teammeet.services.xmpp;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;

import android.util.Log;
import de.teammeet.interfaces.IXMPPService;

public class KeyExchangeListener implements MessageListener {

	private static final String CLASS = KeyExchangeListener.class.getSimpleName();
	
	private IXMPPService mXMPPService;
	private Team mTeam;

	public KeyExchangeListener(IXMPPService service, Team team) {
		mXMPPService = service;
		mTeam = team;
	}
	
	@Override
	public void processMessage(Chat chat, Message message) {
		String sender = message.getFrom();
		Log.d(CLASS, String.format("Received reply to public key from '%s'", sender));
		if (mTeam.isInvitee(sender)) {
			//TODO: 
			TeamMeetPacketExtension extension = (TeamMeetPacketExtension) message.getExtension(TeamMeetPacketExtension.NAMESPACE);
			if (extension.hasCryptoPacket()) {
				byte[] publicKey = extension.getCryptoPacket().getKey();
				mXMPPService.completeSessionKeyExchange(sender, mTeam, chat, publicKey);
			} else {
				//TODO: Inform user via UI
				Log.e(CLASS, String.format("'%s' send reply to public key without crypto packet!", sender));
			}

		} else {
			//TODO: Inform user via UI
			Log.w(CLASS, String.format("Received bogus public key response from '%s'!", sender));
		}
	}

}
