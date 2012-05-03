package de.teammeet.interfaces;

import de.teammeet.services.xmpp.ChatMessage;

public interface IChatMessageHandler {

	boolean handleMessage(ChatMessage message);

}
