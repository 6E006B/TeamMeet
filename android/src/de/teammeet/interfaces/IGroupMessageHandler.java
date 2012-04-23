package de.teammeet.interfaces;

import de.teammeet.xmpp.GroupChatMessage;

public interface IGroupMessageHandler {

	boolean handleGroupMessage(GroupChatMessage message);
}
