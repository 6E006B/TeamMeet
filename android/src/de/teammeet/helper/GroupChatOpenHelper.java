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

public class GroupChatOpenHelper extends SQLiteOpenHelper {

	private static final String CLASS = GroupChatOpenHelper.class.getSimpleName();

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


	public GroupChatOpenHelper(Context context) {
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

	public void addMessage(String from, String group, long timestamp, String message) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(KEY_FROM, from);
		cv.put(KEY_TO, group);
		cv.put(KEY_TIMESTAMP, timestamp);
		cv.put(KEY_MESSAGE, message);
		db.insert(CHAT_TABLE_NAME, null, cv);
		db.close();
	}

	public List<ChatMessage> getMessages(String group) {
		List<ChatMessage> messages = new ArrayList<ChatMessage>();
		SQLiteDatabase db = getReadableDatabase();
		String [] columns = new String[]{KEY_FROM, KEY_TIMESTAMP, KEY_MESSAGE};
		Cursor c = db.query(CHAT_TABLE_NAME, columns, KEY_TO + "=?", new String[]{group},
		                    null, null, KEY_TIMESTAMP);
		while(c.moveToNext()) {
			final String from = c.getString(c.getColumnIndex(KEY_FROM));
			final long timestamp = c.getLong(c.getColumnIndex(KEY_TIMESTAMP));
			final String message = c.getString(c.getColumnIndex(KEY_MESSAGE));
			messages.add(new ChatMessage(from, group, timestamp, message));
		}
		return messages;
	}
}
