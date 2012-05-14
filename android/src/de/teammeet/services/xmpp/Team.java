package de.teammeet.services.xmpp;

import org.jivesoftware.smackx.muc.MultiUserChat;

public class Team {
	
	private MultiUserChat mRoom;
	
	public Team(MultiUserChat room) {
		mRoom = room;
	}
	
	public MultiUserChat getRoom() {
		return mRoom;
	}
}
