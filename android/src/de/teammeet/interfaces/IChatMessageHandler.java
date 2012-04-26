package de.teammeet.interfaces;

import de.teammeet.xmpp.ChatMessage;

public interface IChatMessageHandler {

	boolean handleMessage(ChatMessage message);

}
