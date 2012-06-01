package de.teammeet.services.xmpp;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.InvitationRejectionListener;

import android.util.Log;
import de.teammeet.helper.ToastHelper;
import de.teammeet.interfaces.IXMPPService;

public class TeamJoinDeclinedListener implements InvitationRejectionListener {

	private static final String CLASS = TeamJoinDeclinedListener.class.getSimpleName();

	private IXMPPService mXMPPService;
	private String mTeamName;
	private ToastHelper mToaster;


	public TeamJoinDeclinedListener(IXMPPService service, String teamName) {
		mXMPPService = service;
		mTeamName = teamName;
		mToaster = new ToastHelper();
	}
	
	@Override
	public void invitationDeclined(String invitee, String reason) {
		String info = String.format("'%s' declined to join team '%s'", invitee, mTeamName);
		mToaster.toast(info);
		Log.d(CLASS , info);
		try {
			Team team = mXMPPService.getTeam(mTeamName);
			if (team.isInvitee(invitee)) {
				Log.d(CLASS, String.format("Removing invitee '%s' from list for team '%s'", invitee, mTeamName));
				team.removeInvitee(invitee);
			}
		} catch (XMPPException e) {
			String problem = String.format("Failed to resolve declined team '%s': %s", mTeamName, e.getMessage());
			mToaster.toast(problem);
			Log.e(CLASS, problem);
		}
	}

}
