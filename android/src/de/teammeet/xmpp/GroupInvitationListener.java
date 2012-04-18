package de.teammeet.xmpp;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.InvitationListener;

public class GroupInvitationListener implements InvitationListener {

	@Override
	public void invitationReceived(Connection xmppConnection, String room, String inviter,
			String reason, String password, Message message) {
		// TODO Auto-generated method stub

	}

}
