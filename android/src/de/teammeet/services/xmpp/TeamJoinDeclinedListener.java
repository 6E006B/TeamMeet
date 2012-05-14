package de.teammeet.services.xmpp;

import org.jivesoftware.smackx.muc.InvitationRejectionListener;

import android.util.Log;

public class TeamJoinDeclinedListener implements InvitationRejectionListener {

	private static final String CLASS = TeamJoinDeclinedListener.class.getSimpleName();

	private String mTeamName;  

	public TeamJoinDeclinedListener(String teamName) {
		mTeamName = teamName;
	}
	
	@Override
	public void invitationDeclined(String invitee, String reason) {
		String info = String.format("%s declined to join team %s: %s", invitee, mTeamName, reason);
		Log.d(CLASS , info);
		//TODO Notify user of rejected invitation (in UI thread)
	}

}
