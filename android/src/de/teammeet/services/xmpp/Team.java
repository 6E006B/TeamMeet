package de.teammeet.services.xmpp;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smackx.muc.MultiUserChat;

import android.util.Log;

public class Team {
	private static final String CLASS = Team.class.getSimpleName();
	
	private final String mName;
	private final MultiUserChat mRoom;
	private KeyExchangePartner mInviter;
	private Map<String, KeyExchangePartner> mInvitees;

	
	public Team(String name, MultiUserChat room) {
		mName = name;
		mRoom = room;
		mInviter = null;
		mInvitees = new HashMap<String, KeyExchangePartner>();
	}
	
	public MultiUserChat getRoom() {
		return mRoom;
	}

	public void setInviter(String name) {
		Log.d(CLASS, String.format("Setting inviter for team '%s' to '%s'", mName, name));
		mInviter = new KeyExchangePartner(name);
	}

	public boolean isInviter(String name) {
		boolean isInviter = false;
		if (mInviter != null) {
			isInviter = name.equals(mInviter.getName());
		}
		Log.d(CLASS, String.format("'%s' is inviter: '%s'", name, isInviter));
		return isInviter;
	}

	public KeyExchangePartner getInviter() throws TeamException {
		if (mInviter == null) {
			throw new TeamException("No inviter set");
		}
		return mInviter;
	}

	public void addInvitee(String name) {
		Log.d(CLASS, String.format("Adding invitee '%s'", name));
		mInvitees.put(name, new KeyExchangePartner(name));
	}

	public boolean isInvitee(String name) {
		boolean invited = mInvitees.keySet().contains(name);
		Log.d(CLASS, String.format("'%s' is invitee: '%s'", name, invited));
		return invited;
	}

	public KeyExchangePartner getInvitee(String name) throws TeamException {
		KeyExchangePartner invitee = mInvitees.get(name);
		if (invitee == null) {
			throw new TeamException(String.format("No invitee '%s'", name));
		}
		return invitee;
	}

	public void removeInvitee(String name) {
		Log.d(CLASS, String.format("Removing invitee: '%s'", name));
		mInvitees.remove(name);
	}

	@Override
	public String toString() {
		return mName;
	}

	public static class TeamException extends Exception {
		private static final long serialVersionUID = 8323465405386250301L;

		public TeamException(String reason) {
			super(reason);
		}
	}
}
