package de.teammeet.services.xmpp;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smackx.muc.MultiUserChat;

import android.util.Log;

public class Team {
	private static final String CLASS = Team.class.getSimpleName();
	
	private MultiUserChat mRoom;
	private Map<String, Invitee> mInvitees;
	
	public Team(MultiUserChat room) {
		mRoom = room;
		mInvitees = new HashMap<String, Invitee>();
	}
	
	public MultiUserChat getRoom() {
		return mRoom;
	}

	public void addInvitee(String name) {
		Log.d(CLASS, String.format("Adding invitee '%s'", name));
		mInvitees.put(name, new Invitee(name));
	}

	public boolean isInvitee(String name) {
		boolean invited = mInvitees.keySet().contains(name);
		Log.d(CLASS, String.format("'%s' is invitee: '%s'", name, invited));
		return invited;
	}

	public Invitee getInvitee(String name) throws TeamException {
		Invitee invitee = mInvitees.get(name);
		if (invitee == null) {
			throw new TeamException(String.format("No invitee '%s'", name));
		}
		return invitee;
	}

	public void removeInvitee(String name) {
		Log.d(CLASS, String.format("Removing invitee: '%s'", name));
		mInvitees.remove(name);
	}

	public static class TeamException extends Exception {
		private static final long serialVersionUID = 8323465405386250301L;

		public TeamException(String reason) {
			super(reason);
		}
	}
}
