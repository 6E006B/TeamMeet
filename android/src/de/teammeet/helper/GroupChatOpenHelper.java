package de.teammeet.helper;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import de.teammeet.xmpp.GroupChatMessage;

public class GroupChatOpenHelper extends SQLiteOpenHelper {

	private static final String CLASS = GroupChatOpenHelper.class.getSimpleName();

	private static final String DATABASE_NAME = "TeamMeetGroupChats";
	private static final int DATABASE_VERSION = 1;
	private static final String GROUP_CHAT_TABLE_NAME = "groupChatMessages";
	private static final String KEY_FROM = "sender";
	private static final String KEY_GROUP = "team";
	private static final String KEY_TIMESTAMP = "ts";
	private static final String KEY_MESSAGE = "message";
	private static final String GROUP_CHAT_TABLE_CREATE =
            "CREATE TABLE " + GROUP_CHAT_TABLE_NAME + " (" +
            KEY_FROM + " TEXT, " +
            KEY_GROUP + " TEXT, " +
            KEY_TIMESTAMP + " LONG, " +
            KEY_MESSAGE + " TEXT);";


	public GroupChatOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(GROUP_CHAT_TABLE_CREATE);

	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		Log.w(CLASS, "No upgrading of database implemented!");
	}

	public void addMessage(GroupChatMessage message) {
		addMessage(message.getFrom(),
		           message.getGroup(),
		           message.getTimestamp(),
		           message.getMessage());
	}
	
	public void addMessage(String from, String group, long timestamp, String message) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(KEY_FROM, from);
		cv.put(KEY_GROUP, group);
		cv.put(KEY_TIMESTAMP, timestamp);
		cv.put(KEY_MESSAGE, message);
		db.insert(GROUP_CHAT_TABLE_NAME, null, cv);
		db.close();
	}

	public List<GroupChatMessage> getMessages(String group) {
		List<GroupChatMessage> messages = new ArrayList<GroupChatMessage>();
		SQLiteDatabase db = getReadableDatabase();
		String [] columns = new String[]{KEY_FROM, KEY_TIMESTAMP, KEY_MESSAGE};
		Cursor c = db.query(GROUP_CHAT_TABLE_NAME, columns, KEY_GROUP + "=?", new String[]{group},
		                    null, null, KEY_TIMESTAMP);
		while(c.moveToNext()) {
			final String from = c.getString(c.getColumnIndex(KEY_FROM));
			final long timestamp = c.getLong(c.getColumnIndex(KEY_TIMESTAMP));
			final String message = c.getString(c.getColumnIndex(KEY_MESSAGE));
			messages.add(new GroupChatMessage(from, group, timestamp, message));
		}
		return messages;
	}
}
