package de.teammeet.services.xmpp;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;

import android.util.Log;
import de.teammeet.interfaces.IXMPPService;

public class TeamJoinListener implements ParticipantStatusListener {

	private static final String CLASS = TeamJoinListener.class.getSimpleName();
	
	private IXMPPService mXMPPService; 
	private Team mTeam;


	public TeamJoinListener(IXMPPService service, Team team) {
		mXMPPService = service;
		mTeam = team;
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
		Log.d(CLASS, String.format("%s just joined team '%s'", fullAddress, mTeam));

		try {
			String fullJID = getFullJID(mTeam, fullAddress);
			Log.d(CLASS, String.format("full JID is '%s'", fullJID));
			String mate = StringUtils.parseBareAddress(fullJID);

			if (mTeam.isInvitee(mate)) {
				Log.d(CLASS, String.format("Initiating session key exchange for team '%s' with '%s'", mTeam, mate));
				mTeam.removeInvitee(mate);
				mXMPPService.initiateSessionKeyExchange(mate, mTeam);
			}
		} catch (XMPPException e) {
			//TODO: Notify user via UI
			Log.e(CLASS, String.format("Failed to get team '%s': %s", mTeam, e.getMessage()));
			return;
		}
	}

	private String getFullJID(Team team, String fullNick) throws XMPPException {
		String fullJID = null;

		Occupant occupant = team.getRoom().getOccupant(fullNick);
		if (occupant != null) {
			fullJID = occupant.getJid();
			if (fullJID == null) {
				throw new XMPPException(String.format("Full JID for '%s' not available in '%s'",
													   fullNick, team));
			}
		} else {
			throw new XMPPException(String.format("No user '%s' in '%s'", fullNick, team));
		}

		return fullJID;
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
