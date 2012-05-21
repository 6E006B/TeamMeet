package de.teammeet.services.xmpp;

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
			byte[] publicKey = extension.getCryptoPacket().getKey();
			String teamName = extension.getCryptoPacket().getTeam();
			try {
				Team team = mXMPPService.getTeam(teamName);
				Log.d(CLASS, String.format("Checking legitimacy of crypto packet from '%s' in '%s'", sender, team));
				if (team.isInvitee(sender)) {
					KeyExchangePartner invitee = team.getInvitee(sender);
					invitee.calculateSharedSecret(publicKey);
				} else if (team.isInviter(sender)) {
					KeyExchangePartner inviter = team.getInviter();
					mXMPPService.sendKey(sender, TeamMeetPacketExtension.KEYTYPE_PUBLIC, inviter.getPublicKey(), teamName);
					inviter.calculateSharedSecret(publicKey);
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
			}
		} else {
			//TODO: Inform user via UI
			Log.e(CLASS, String.format("Protocol breach: '%s' sent teammeet packet through chat without crypto packet!", sender));
		}
	}

}
