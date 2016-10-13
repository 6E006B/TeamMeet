package de.teammeet.interfaces;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.packet.Message;

public interface IInvitationHandler {
	boolean handleInvitation(Connection connection,
                             String room,
                             String inviter,
                             String password,
                             Message message);
}
