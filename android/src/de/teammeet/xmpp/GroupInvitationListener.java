package de.teammeet.xmpp;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.InvitationListener;

import android.content.SharedPreferences;
import android.util.Log;
import de.teammeet.SettingsActivity;

public class GroupInvitationListener implements InvitationListener {

	private static final String CLASS = GroupInvitationListener.class.getSimpleName();

	private SharedPreferences mSettings = null;
	private XMPPService mXMPPService = null;

	public GroupInvitationListener(SharedPreferences settings, XMPPService xmppService) {
		mSettings = settings;
		mXMPPService = xmppService;
	}

	@Override
	public void invitationReceived(Connection xmppConnection, String room, String inviter,
			String reason, String password, Message message) {
		Log.d(CLASS, String.format("GroupInvitationListener.invitationReceived(... '%s', '%s', " + 
			"'%s', '%s', '%s' ...) from %s", room, inviter, reason, password, message.getFrom()));
		// automatically join when invited
		// TODO inform user about invite and let him decide whether to join or not
		String userID = mSettings.getString(SettingsActivity.SETTING_XMPP_USER_ID, "");
		try {
			mXMPPService.joinGroup(room, userID, password, message.getFrom());
		} catch (XMPPException e) {
			Log.e(CLASS, "GroupInvitationListener.invitationReceived() failed to join group");
			e.printStackTrace();
		}
	}

}
