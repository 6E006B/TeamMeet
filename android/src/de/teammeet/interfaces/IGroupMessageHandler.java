package de.teammeet.interfaces;

import de.teammeet.services.xmpp.ChatMessage;

public interface IGroupMessageHandler {

	boolean handleGroupMessage(ChatMessage message);
}
