package de.teammeet.services.xmpp;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.InvitationListener;

import android.util.Log;

public class RoomInvitationListener implements InvitationListener {

	private static final String CLASS = RoomInvitationListener.class.getSimpleName();

	private XMPPService mXMPPService = null;

	public RoomInvitationListener(XMPPService xmppService) {
		mXMPPService = xmppService;
	}

	@Override
	public void invitationReceived(Connection connection, String room, String inviter,
								   String reason, String password, Message message) {
		Log.d(CLASS, String.format("Received invitation to '%s' [pass: '%s'] from '%s'",
									room, password, inviter));
		mXMPPService.newInvitation(connection, room, inviter, password, message);
	}
}
