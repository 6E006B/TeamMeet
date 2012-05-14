package de.teammeet.services.xmpp;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.InvitationRejectionListener;

import android.util.Log;
import de.teammeet.interfaces.IXMPPService;

public class TeamJoinDeclinedListener implements InvitationRejectionListener {

	private static final String CLASS = TeamJoinDeclinedListener.class.getSimpleName();

	private IXMPPService mXMPPService;
	private String mTeamName;


	public TeamJoinDeclinedListener(IXMPPService service, String teamName) {
		mXMPPService = service;
		mTeamName = teamName;
	}
	
	@Override
	public void invitationDeclined(String invitee, String reason) {
		String info = String.format("%s declined to join team %s: %s", invitee, mTeamName, reason);
		Log.d(CLASS , info);
		//TODO Notify user of rejected invitation (in UI thread)
		try {
			Team team = mXMPPService.getTeam(invitee);
			if (team.isInvitee(invitee)) {
				Log.d(CLASS, String.format("Removing invitee '%s' from list for team '%s'", invitee, mTeamName));
				team.removeInvitee(invitee);
			}
		} catch (XMPPException e) {
			//TODO: Notify user via UI
			Log.e(CLASS, String.format("Failed to get team '%s': %s", mTeamName, e.getMessage()));
		}
	}

}
