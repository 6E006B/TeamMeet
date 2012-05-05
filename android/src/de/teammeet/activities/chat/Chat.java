package de.teammeet.activities.chat;

import java.util.List;

import org.jivesoftware.smack.XMPPException;
import org.xbill.DNS.InvalidTypeException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import de.teammeet.R;
import de.teammeet.helper.ChatOpenHelper;
import de.teammeet.interfaces.IChatMessageHandler;
import de.teammeet.interfaces.IGroupMessageHandler;
import de.teammeet.services.xmpp.ChatMessage;
import de.teammeet.services.xmpp.XMPPService;


public class Chat implements IChatMessageHandler, IGroupMessageHandler {

	public static final int TYPE_NORMAL_CHAT = 1;
	public static final int TYPE_GROUP_CHAT = 2;

	private static final String CLASS = Chat.class.getSimpleName();;

	private int mType;
	private String mCounterpart;
	private String mContact;
	private String mOwnID;
	private String mOwnUsername;
	private XMPPService mXMPPService;
	private ChatActivity mMessageHandler;
	private ChatOpenHelper mDatabase;
	
	public Chat(int type, String counterpart, XMPPService xmppService,
			ChatActivity messageHandler) {
		Log.d(CLASS, String.format("Chat(%d, '%s', ...)", type, counterpart));
		mType = type;
		mCounterpart = counterpart;
		mXMPPService = xmppService;
		mMessageHandler = messageHandler;
		mDatabase = new ChatOpenHelper(mXMPPService);

		mContact = getUsernameAndServer(mCounterpart);

		if (mType == 0) {
			throw new InvalidTypeException(mType);
		}
		if (mCounterpart == null) {
			throw new NullPointerException("Chat didn't contain SENDER");
		}

		final SharedPreferences settings = PreferenceManager.
				getDefaultSharedPreferences(mMessageHandler.getApplicationContext());
		final String userIDKey = mMessageHandler.getString(R.string.preference_user_id_key);
		mOwnUsername = settings.getString(userIDKey, "");
		final String serverKey = mMessageHandler.getString(R.string.preference_server_key);
		final String server = settings.getString(serverKey, "");
		mOwnID = String.format("%s@%s", mOwnUsername, server);
		
		// Just some debug info:
		switch (mType) {
		case TYPE_NORMAL_CHAT:
			Log.d(CLASS, String.format("normal chat with '%s'", mCounterpart));
			break;

		case TYPE_GROUP_CHAT:
			Log.d(CLASS, String.format("group chat in '%s'", mCounterpart));
			break;
		}
	}

	public Chat(Intent intent, XMPPService xmppService, ChatActivity messageHandler) {
		this(intent.getIntExtra(XMPPService.TYPE, 0),
		     intent.getStringExtra(XMPPService.SENDER),
		     xmppService,
		     messageHandler);
	}

	public void setXMPPService(XMPPService xmppService) {
		mXMPPService = xmppService;
	}
	public List<ChatMessage> fetchMessages() {
		List<ChatMessage> messageList = null;
		switch (mType) {
		case TYPE_NORMAL_CHAT:
			messageList = mDatabase.getMessages(mContact);
			break;

		case TYPE_GROUP_CHAT:
			messageList = mDatabase.getMessages(mCounterpart);
			break;
		}
		return messageList;
	}

	public void sendMessage(String message) throws XMPPException {
		if (!message.equals("")) {
			Log.d(CLASS, "sending: " + message);
			switch (mType) {
			case TYPE_NORMAL_CHAT:
				mXMPPService.sendChatMessage(mCounterpart, message);
				break;

			case TYPE_GROUP_CHAT:
				mXMPPService.sendToGroup(mCounterpart, message);
				break;
			}
		} else {
			Log.d(CLASS, "not sending empty message");
		}
	}

	public boolean isConcerned(final ChatMessage message) {
		Log.d(CLASS, "Chat.isConcerned()");
		boolean concerned = false;
		switch (mType) {
		case TYPE_NORMAL_CHAT:
			concerned = message.getTo().startsWith(mContact) ||
						message.getFrom().startsWith(mContact);
			break;

		case TYPE_GROUP_CHAT:
			concerned = mCounterpart.equals(message.getTo());
			break;
		}
		Log.d(CLASS, "concerned: " + Boolean.toString(concerned));
		return concerned;
	}

	public CharSequence createMessageSequence(ChatMessage message) {
		String colour = "red";
		String from = "";
		switch (mType) {
		case TYPE_NORMAL_CHAT:
			if (message.getFrom().startsWith(mOwnID)) {
				colour = "green";
			}
			from = getUsername(message.getFrom());
			break;

		case TYPE_GROUP_CHAT:
			//TODO: this is probably not a reliable way to do this, since the own username might be
			// 		already taken when joining a MUC. Currently this probably hinders joining the
			//		MUC at all, but we probably want to change that.
			if (message.getFrom().endsWith(String.format("/%s", mOwnUsername))) {
				colour = "green";
			}
			from = getPath(message.getFrom());
			break;
		}

		final String sender = String.format("<b><font color=\"%s\">%s:</font></b> ", colour, from);
		final Spanned senderHTML = Html.fromHtml(sender);
		Log.d(CLASS, "inhere");
		return TextUtils.concat(senderHTML, message.getMessage());
	}

	public static String getUsername(String jid) {
		return jid.substring(0, jid.indexOf('@'));
	}

	public static String getUsernameAndServer(String jid) {
		final int slashIndex = jid.indexOf('/');
		if (slashIndex != -1) {
			jid = jid.substring(0, slashIndex);
		}
		return jid;
	}
	public static String getPath(String jid) {
		return jid.substring(jid.lastIndexOf('/') + 1);
	}

	@Override
	public boolean handleGroupMessage(ChatMessage message) {
		boolean handled = false;
		if (mType == TYPE_GROUP_CHAT) {
			Log.d(CLASS, "Chat.handleGroupMessage()");
			if (isConcerned(message)) {
				Log.d(CLASS, "isconcerned");
				mMessageHandler.handleMessage(createMessageSequence(message));
				handled = true;
			}
		}
		return handled;
	}

	@Override
	public boolean handleMessage(ChatMessage message) {
		boolean handled = false;
		if (mType == TYPE_NORMAL_CHAT) {
			Log.d(CLASS, "Chat.handleMessage()");
			if (isConcerned(message)) {
				mMessageHandler.handleMessage(createMessageSequence(message));
				handled = true;
			}
		}
		return handled;
	}
}
