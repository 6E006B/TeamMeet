package de.teammeet.services.xmpp;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;

import android.util.Log;
import de.teammeet.helper.ToastHelper;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.services.xmpp.Team.TeamException;

public class TeamJoinListener implements ParticipantStatusListener {

	private static final String CLASS = TeamJoinListener.class.getSimpleName();
	
	private IXMPPService mXMPPService; 
	private Team mTeam;
	private ToastHelper mToaster;


	public TeamJoinListener(XMPPService service, Team team) {
		mXMPPService = service;
		mTeam = team;
		mToaster = new ToastHelper();
	}
	
	@Override
	public void adminGranted(String arg0) {}

	@Override
	public void adminRevoked(String arg0) {}

	@Override
	public void banned(String arg0, String arg1, String arg2) {}

	@Override
	public void joined(String fullAddress) {
		String mateName = null;
		try {
			String fullJID = getFullJID(mTeam, fullAddress);
			Log.d(CLASS, String.format("full JID is '%s'", fullJID));
			mateName = StringUtils.parseBareAddress(fullJID);

			String status = String.format("%s just joined team '%s'",
										   StringUtils.parseName(mateName),
										   StringUtils.parseName(mTeam.toString()));
			mToaster.toast(status);
			Log.d(CLASS, status);

			if (mTeam.isInvitee(mateName)) {
				status = String.format("Exchanging public keys...",
										StringUtils.parseName(mateName),
										StringUtils.parseName(mTeam.toString()));
				mToaster.toast(status);
				Log.d(CLASS, status);

				KeyExchangePartner mate = mTeam.getInvitee(mateName);
				mXMPPService.sendKey(mate.getName(), TeamMeetPacketExtension.KEYTYPE_PUBLIC, mate.getPublicKey(), mTeam.toString());
			}
		} catch (XMPPException e) {
			String problem = String.format("Failed to resolve full JID for '%s' in '%s': %s", fullAddress, mTeam, e.getMessage());
			mToaster.toast(problem);
			Log.e(CLASS, problem);
		} catch (TeamException e) {
			String problem = String.format("Could not get mate '%s' from team '%s': %s", mateName, mTeam, e.getMessage());
			mToaster.toast(problem);
			Log.e(CLASS, problem);
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
	public void kicked(String arg0, String arg1, String arg2) {}

	@Override
	public void left(String arg0) {}

	@Override
	public void membershipGranted(String arg0) {}

	@Override
	public void membershipRevoked(String arg0) {}

	@Override
	public void moderatorGranted(String arg0) {}

	@Override
	public void moderatorRevoked(String arg0) {}

	@Override
	public void nicknameChanged(String arg0, String arg1) {}

	@Override
	public void ownershipGranted(String arg0) {}

	@Override
	public void ownershipRevoked(String arg0) {}

	@Override
	public void voiceGranted(String arg0) {}

	@Override
	public void voiceRevoked(String arg0) {}

}
