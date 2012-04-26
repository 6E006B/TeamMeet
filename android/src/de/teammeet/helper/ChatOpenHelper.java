package de.teammeet.helper;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import de.teammeet.xmpp.ChatMessage;

public class ChatOpenHelper extends SQLiteOpenHelper {

	private static final String CLASS = ChatOpenHelper.class.getSimpleName();

	private static final String DATABASE_NAME = "TeamMeetChats";
	private static final int DATABASE_VERSION = 1;
	private static final String CHAT_TABLE_NAME = "chatMessages";
	private static final String KEY_FROM = "sender";
	private static final String KEY_TO = "receipient";
	private static final String KEY_TIMESTAMP = "ts";
	private static final String KEY_MESSAGE = "message";
	private static final String CHAT_TABLE_CREATE =
            "CREATE TABLE " + CHAT_TABLE_NAME + " (" +
            KEY_FROM + " TEXT, " +
            KEY_TO + " TEXT, " +
            KEY_TIMESTAMP + " LONG, " +
            KEY_MESSAGE + " TEXT);";


	public ChatOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CHAT_TABLE_CREATE);

	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		Log.w(CLASS, "No upgrading of database implemented!");
	}

	public void addMessage(ChatMessage message) {
		addMessage(message.getFrom(),
		           message.getTo(),
		           message.getTimestamp(),
		           message.getMessage());
	}

	/**
	 * Writes a new message to the SQLite database
	 * @param from The sender of the message.
	 * @param to The receipient of the message. For groups the receipient is the group itself.
	 * @param timestamp
	 * @param message
	 */
	public void addMessage(String from, String to, long timestamp, String message) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(KEY_FROM, from);
		cv.put(KEY_TO, to);
		cv.put(KEY_TIMESTAMP, timestamp);
		cv.put(KEY_MESSAGE, message);
		db.insert(CHAT_TABLE_NAME, null, cv);
		db.close();
	}

	/**
	 * 
	 * @param conversationPartner The partner of the conversation to fetch the messages for.
	 *                            This is the room for group chats.
	 * @return A list of chat messages with the conversation partner.
	 */
	public List<ChatMessage> getMessages(String conversationPartner) {
		final List<ChatMessage> messages = new ArrayList<ChatMessage>();
		final SQLiteDatabase db = getReadableDatabase();
		final String [] columns = new String[]{KEY_FROM, KEY_TO, KEY_TIMESTAMP, KEY_MESSAGE};
		final String whereClause = String.format("%s=? OR %s=?", KEY_TO, KEY_FROM);
		final Cursor c = db.query(CHAT_TABLE_NAME, columns, whereClause,
		                    new String[]{conversationPartner, conversationPartner},
		                    null, null, KEY_TIMESTAMP);
		while(c.moveToNext()) {
			final String from = c.getString(c.getColumnIndex(KEY_FROM));
			final long timestamp = c.getLong(c.getColumnIndex(KEY_TIMESTAMP));
			final String message = c.getString(c.getColumnIndex(KEY_MESSAGE));
			messages.add(new ChatMessage(from, conversationPartner, timestamp, message));
		}
		return messages;
	}
}
