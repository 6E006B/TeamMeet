package de.teammeet.interfaces;

import de.teammeet.xmpp.ChatMessage;

public interface IGroupMessageHandler {

	boolean handleGroupMessage(ChatMessage message);
}
