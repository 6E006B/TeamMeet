package de.teammeet.services.xmpp;

import java.security.InvalidKeyException;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;

import android.util.Log;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.services.xmpp.Team.TeamException;

public class KeyExchangeListener implements PacketListener {

	private static final String CLASS = KeyExchangeListener.class.getSimpleName();
	
	private IXMPPService mXMPPService;

	public KeyExchangeListener(IXMPPService service) {
		mXMPPService = service;
	}
	
	@Override
	public void processPacket(Packet packet) {
		String sender = StringUtils.parseBareAddress(packet.getFrom());
		Log.d(CLASS, String.format("Received crypto packet from '%s'", sender));
		TeamMeetPacketExtension extension = (TeamMeetPacketExtension) packet.getExtension(TeamMeetPacketExtension.NAMESPACE);
		if (extension.hasCryptoPacket()) {
			CryptoPacket cryptoPacket = extension.getCryptoPacket();
			byte[] key = cryptoPacket.getKey();
			String teamName = cryptoPacket.getTeam();
			try {
				Team team = mXMPPService.getTeam(teamName);
				Log.d(CLASS, String.format("Checking legitimacy of crypto packet from '%s' in '%s'", sender, team));

				if (team.isInvitee(sender)) {
					if (cryptoPacket.isPublicKey()) {

						byte[] dummySessionKey = new byte[] { 1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16,
								 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32};

						byte[] encryptedKey = null;
						KeyExchangePartner invitee = team.getInvitee(sender);
						invitee.calculateSharedSecret(key);
						encryptedKey = invitee.encryptSessionKey(dummySessionKey);
						mXMPPService.sendKey(sender, TeamMeetPacketExtension.KEYTYPE_SECRET, encryptedKey, teamName);
					} else {
						//TODO: Inform user via UI
						Log.w(CLASS, String.format("Protocol breach: Received session key from invitee '%s'", sender));
					}

				} else if (team.isInviter(sender)) {
					if (cryptoPacket.isPublicKey()) {
						KeyExchangePartner inviter = team.getInviter();
						mXMPPService.sendKey(sender, TeamMeetPacketExtension.KEYTYPE_PUBLIC, inviter.getPublicKey(), teamName);
						inviter.calculateSharedSecret(key);
					} else {
						Log.d(CLASS, String.format("Received session key from inviter '%s' [not implemented]", sender));
					}

				} else {
					//TODO: Inform user via UI
					Log.w(CLASS, String.format("Received bogus public key from '%s'!", sender));
				}
			} catch (XMPPException e) {
				//TODO: Inform user via UI
				Log.e(CLASS, String.format("'%s' sent crypto packet for unknown team '%s': %s", sender, teamName, e.getMessage()));
			} catch (TeamException e) {
				//TODO: Inform user via UI
				Log.e(CLASS, String.format("Could not get exchange partner in '%s': %s", teamName, e.getMessage()));
			} catch (InvalidKeyException e) {
				//TODO: Inform user via UI
				Log.e(CLASS, String.format("Could not calculate key for '%s': %s", sender, e.getMessage()));
			}
		} else {
			//TODO: Inform user via UI
			Log.e(CLASS, String.format("Protocol breach: '%s' sent teammeet packet through chat without crypto packet!", sender));
		}
	}
}
