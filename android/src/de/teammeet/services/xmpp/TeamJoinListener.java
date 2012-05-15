package de.teammeet.services.xmpp;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;

import android.util.Log;
import de.teammeet.interfaces.IXMPPService;

public class TeamJoinListener implements ParticipantStatusListener {

	private static final String CLASS = TeamJoinListener.class.getSimpleName();
	
	private IXMPPService mXMPPService; 
	private String mTeamName;


	public TeamJoinListener(IXMPPService service, String teamName) {
		mXMPPService = service;
		mTeamName = teamName;
	}
	
	@Override
	public void adminGranted(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void adminRevoked(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void banned(String arg0, String arg1, String arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void joined(String fullAddress) {
		Log.d(CLASS, String.format("%s just joined team '%s'", fullAddress, mTeamName));
		try {
			Team team = mXMPPService.getTeam(mTeamName);
			String fullJID = mXMPPService.getFullJID(mTeamName, fullAddress);
			Log.d(CLASS, String.format("full JID is '%s'", fullJID));
			String mate = StringUtils.parseBareAddress(fullJID);
			if (team.isInvitee(mate)) {
				Log.d(CLASS, String.format("Initiating session key exchange for team '%s' with '%s'", mTeamName, mate));
				team.removeInvitee(mate);
			}
		} catch (XMPPException e) {
			//TODO: Notify user via UI
			Log.e(CLASS, String.format("Failed to get team '%s': %s", mTeamName, e.getMessage()));
		}
	}

	@Override
	public void kicked(String arg0, String arg1, String arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void left(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void membershipGranted(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void membershipRevoked(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void moderatorGranted(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void moderatorRevoked(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void nicknameChanged(String arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ownershipGranted(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ownershipRevoked(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void voiceGranted(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void voiceRevoked(String arg0) {
		// TODO Auto-generated method stub

	}

}
