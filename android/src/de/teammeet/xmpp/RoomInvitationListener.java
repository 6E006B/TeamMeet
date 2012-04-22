package de.teammeet.xmpp;

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
        Log.d(CLASS, "GroupInvitationListener.invitationReceived(... '" + room + "', '" + inviter +
                     "', '" + reason + "', '" +  password + "' ...) from " + message.getFrom());
        mXMPPService.newInvitation(connection, room, inviter, reason, password, message);
	}
}
