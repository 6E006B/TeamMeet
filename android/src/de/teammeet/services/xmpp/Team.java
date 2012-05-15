package de.teammeet.services.xmpp;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smackx.muc.MultiUserChat;

import android.util.Log;

public class Team {
	private static final String CLASS = Team.class.getSimpleName();
	
	private MultiUserChat mRoom;
	private List<String> mInvitees;
	
	public Team(MultiUserChat room) {
		mRoom = room;
		mInvitees = new ArrayList<String>();
	}
	
	public MultiUserChat getRoom() {
		return mRoom;
	}

	public void addInvitee(String invitee) {
		Log.d(CLASS, String.format("Adding invitee '%s'", invitee));
		mInvitees.add(invitee);

	}

	public boolean isInvitee(String invitee) {
		Log.d(CLASS, String.format("'%s' is invitee: '%s'", invitee, mInvitees.contains(invitee)));
		return mInvitees.contains(invitee);
	}

	public void removeInvitee(String invitee) {
		Log.d(CLASS, String.format("Removing invitee: '%s'", invitee));
		mInvitees.remove(invitee);
	}
}
