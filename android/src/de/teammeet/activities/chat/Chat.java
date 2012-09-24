package de.teammeet.activities.chat;

import java.util.List;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
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
	private ChatFragment mMessageHandler;
	private ChatOpenHelper mDatabase;
	
	public Chat(int type, String counterpart, ChatFragment messageHandler) {
		Log.d(CLASS, String.format("Chat(%d, '%s', ...)", type, counterpart));
		mType = type;
		mCounterpart = counterpart;
		mMessageHandler = messageHandler;
		mDatabase = new ChatOpenHelper(mMessageHandler.getActivity().getApplicationContext());

		mContact = getUsernameAndServer(mCounterpart);

		if (mType == 0) {
			throw new InvalidTypeException(mType);
		}
		if (mCounterpart == null) {
			throw new NullPointerException("Chat didn't contain SENDER");
		}

		final SharedPreferences settings = PreferenceManager.
				getDefaultSharedPreferences(mMessageHandler.getActivity().getApplicationContext());
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

	public Chat(Intent intent, ChatFragment messageHandler) {
		this(intent.getIntExtra(XMPPService.TYPE, 0),
		     intent.getStringExtra(XMPPService.SENDER),
		     messageHandler);
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

	public void sendMessage(String message, XMPPService xmppService) throws XMPPException {
		if (!message.equals("")) {
			Log.d(CLASS, "sending: " + message);
			switch (mType) {
			case TYPE_NORMAL_CHAT:
				xmppService.sendChatMessage(mCounterpart, message);
				break;

			case TYPE_GROUP_CHAT:
				xmppService.sendToTeam(mCounterpart, message);
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

	public static String getUsername(String jid) {
		return StringUtils.parseName(jid);
	}

	public static String getUsernameAndServer(String jid) {
		return StringUtils.parseBareAddress(jid);
	}

	public static String getResource(String jid) {
		return StringUtils.parseResource(jid);
	}

	@Override
	public boolean handleGroupMessage(ChatMessage message) {
		boolean handled = false;
		if (mType == TYPE_GROUP_CHAT) {
			Log.d(CLASS, "Chat.handleGroupMessage()");
			if (isConcerned(message)) {
				mMessageHandler.handleMessage(new ChatEntry(message));
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
				mMessageHandler.handleMessage(new ChatEntry(message));
				handled = true;
			}
		}
		return handled;
	}

	protected class ChatEntry {

		private String mText;
		private String mSender;
		private boolean mFromMe = false;

		public ChatEntry(ChatMessage message) {
			String from = message.getFrom();
			mText = message.getMessage();

			switch (mType) {
			case TYPE_NORMAL_CHAT:
				mFromMe = from.startsWith(mOwnID);
				mSender = getUsername(from);
				break;

			case TYPE_GROUP_CHAT:
				//TODO: this is probably not a reliable way to do this, since the own username might be
				// 		already taken when joining a MUC. Currently this probably hinders joining the
				//		MUC at all, but we probably want to change that.
				mFromMe = from.endsWith(String.format("/%s", mOwnUsername));
				mSender = getResource(from);
				break;
			}
		}

		public CharSequence format() {
			int userColour = mFromMe ? R.color.chatMyUsername : R.color.chatOtherUsername;
			final String sender = String.format("<b><font color=\"%s\">%s:</font></b> ",
					                            mMessageHandler.getResources().getColor(userColour),
					                            mSender);
			final Spanned senderHTML = Html.fromHtml(sender);
			return TextUtils.concat(senderHTML, mText);
		}

		public boolean isFromMe() {
			return mFromMe;
		}
	}
}
