package de.teammeet.activities.chat;

import java.util.List;

import org.jivesoftware.smack.XMPPException;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import de.teammeet.R;
import de.teammeet.helper.ChatOpenHelper;
import de.teammeet.interfaces.IGroupMessageHandler;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.services.xmpp.ChatMessage;
import de.teammeet.services.xmpp.XMPPService;

public class GroupChatActivity extends Activity implements IGroupMessageHandler {

	private static final String CLASS = GroupChatActivity.class.getSimpleName();

	private ListView mChatListView = null;
	private ArrayAdapter<CharSequence> mListAdapter = null;
	private EditText mChatEditText = null;
	private String mGroup = null;
	private ChatOpenHelper mDatabase = null;
	private String mUserID = null;

	private IXMPPService mXMPPService = null;
	private XMPPServiceConnection mXMPPServiceConnection = new XMPPServiceConnection();

	private class XMPPServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(CLASS, "RosterActivity.XMPPServiceConnection.onServiceConnected('" +
						 className + "')");
			mXMPPService = ((XMPPService.LocalBinder) binder).getService();
			mXMPPService.registerGroupMessageHandler(GroupChatActivity.this);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			Log.d(CLASS, "RosterActivity.XMPPServiceConnection.onServiceDisconnected('" +
						 className + "')");
			mXMPPService = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS, "onCreate(): started group chat");
		
		setContentView(R.layout.chat);

		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		final String userIDKey = getString(R.string.preference_user_id_key);
		mUserID = settings.getString(userIDKey, "");

		mChatListView = (ListView)findViewById(R.id.chatListView);
		mListAdapter = new ArrayAdapter<CharSequence>(this, R.layout.chat_item);
		mChatListView.setAdapter(mListAdapter);
		mChatEditText = (EditText)findViewById(R.id.chatInput);
		mChatEditText.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				String sendText = v.getText().toString();
				if (!sendText.equals("")) {
					Log.d(CLASS, "sending: " + sendText);
					try {
						mXMPPService.sendToGroup(mGroup, sendText);
					} catch (XMPPException e) {
						final String errorMessage = "Unable to send message:\n" + e.getMessage();
						Log.e(CLASS, errorMessage);
						e.printStackTrace();
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								final Toast toast = Toast.makeText(GroupChatActivity.this,
								                                   errorMessage, Toast.LENGTH_LONG);
								toast.setGravity(Gravity.BOTTOM, 0, 0);
								toast.show();
							}
						});
					}
					v.setText("");
				} else {
					Log.d(CLASS, "not sending empty message");
				}
				return true;
			}
		});

		mDatabase = new ChatOpenHelper(getApplicationContext());
		handleIntent(getIntent());
	}

	@Override
	protected void onResume() {
		super.onResume();

		// create the service (if it isn't already running)
		final Intent xmppIntent = new Intent(getApplicationContext(), XMPPService.class);
		startService(xmppIntent);

		Log.d(CLASS, "started XMPP service");

		// now connect to the service
		boolean bindSuccess = bindService(xmppIntent, mXMPPServiceConnection, 0);
		if (bindSuccess) {
			Log.d(CLASS, "onResume(): bind to XMPP service succeeded");
		} else {
			Log.e(CLASS, "onResume(): bind to XMPP service failed");
			Toast.makeText(getApplicationContext(), "Couldn't connect to XMPP service.", 3);
		}
	}

	@Override
	protected void onPause() {
		mXMPPService.unregisterGroupMessageHandler(this);
		if (mXMPPServiceConnection != null) {
			unbindService(mXMPPServiceConnection);
		}
		mXMPPService = null;
		super.onPause();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(CLASS, "ChatActivity.onNewIntent()");
		super.onNewIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		mGroup = intent.getStringExtra(XMPPService.GROUP);
		if (mGroup != null) {
			List<ChatMessage> messages = mDatabase.getMessages(mGroup);
			for (ChatMessage message : messages) {
				mListAdapter.add(createMessageSequence(message));
			}
			mListAdapter.notifyDataSetChanged();
			mChatListView.setSelection(mListAdapter.getCount());
		} else {
			final String error = "GroupChatActivity intent has no group";
			Log.e(CLASS, error);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(GroupChatActivity.this, error, Toast.LENGTH_LONG).show();
				}
			});
		}
	}

	@Override
	public boolean handleGroupMessage(final ChatMessage message) {
		Log.d(CLASS, "GroupChatActivity.handleGroupMessage()");
		boolean handled = false;
		if (message.getTo().equals(mGroup)) {
			mChatListView.post(new Runnable() {
				@Override
				public void run() {
					mListAdapter.add(createMessageSequence(message));
					mListAdapter.notifyDataSetChanged();
					Log.d(CLASS, "list adapter count is "+mListAdapter.getCount());
					mChatListView.setSelection(mListAdapter.getCount());
				}
			});
			handled = true;
		}
		return handled;
	}

	private CharSequence createMessageSequence(ChatMessage message) {
		String colour = "red";
		if (message.getFrom().endsWith(String.format("/%s", mUserID))) {
			colour = "green";
		}
		final String from = message.getFrom().substring(message.getFrom().lastIndexOf('/') + 1);
		final String sender = String.format("<b><font color=\"%s\">%s:</font></b> ", colour, from);
		final Spanned senderHTML = Html.fromHtml(sender);
		return TextUtils.concat(senderHTML, message.getMessage());
	}
}
