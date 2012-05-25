package de.teammeet.services.xmpp;

import java.security.InvalidKeyException;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;

import android.util.Log;
import de.teammeet.helper.ToasterHelper;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.services.xmpp.Team.TeamException;

public class KeyExchangeListener implements PacketListener {

	private static final String CLASS = KeyExchangeListener.class.getSimpleName();

	private IXMPPService mXMPPService;
	private ToasterHelper mToaster;

	public KeyExchangeListener(XMPPService service) {
		mXMPPService = service;
		mToaster = new ToasterHelper(service);
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
				String status = String.format("Checking legitimacy of crypto packet from '%s' in '%s'", sender, team);
				Log.d(CLASS, status);

				if (team.isInvitee(sender)) {
					if (cryptoPacket.isPublicKey()) {
						status = "Sending session key";
						Log.d(CLASS, status);
						mToaster.toast(status);

						byte[] dummySessionKey = "This is just a dummy session key".getBytes();

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
					KeyExchangePartner inviter = team.getInviter();
					if (cryptoPacket.isPublicKey()) {
						status = "Exchanging public keys...";
						Log.d(CLASS, status);
						mToaster.toast(status);

						mXMPPService.sendKey(sender, TeamMeetPacketExtension.KEYTYPE_PUBLIC, inviter.getPublicKey(), teamName);
						inviter.calculateSharedSecret(key);
					} else {
						status = "Received session key";
						Log.d(CLASS, status);
						mToaster.toast(status);
						byte[] sessionKey = inviter.decryptSessionKey(key);
						Log.d(CLASS, String.format("Decrypted session key: '%s'", new String(sessionKey)));
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
