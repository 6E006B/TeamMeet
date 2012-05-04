package de.teammeet.activities.chat;

import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import de.teammeet.R;
import de.teammeet.helper.ChatOpenHelper;
import de.teammeet.interfaces.IChatMessageHandler;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.services.xmpp.ChatMessage;
import de.teammeet.services.xmpp.XMPPService;

public class ChatActivity extends Activity implements IChatMessageHandler {

	private static final String CLASS = ChatActivity.class.getSimpleName();

	private ListView mChatListView = null;
	private ArrayAdapter<CharSequence> mListAdapter = null;
	private EditText mChatEditText = null;
	private String mSender = null;
	private ChatOpenHelper mDatabase = null;

	private IXMPPService mXMPPService = null;
	private XMPPServiceConnection mXMPPServiceConnection = new XMPPServiceConnection();
	private String mOwnID;

	private class XMPPServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(CLASS, "RosterActivity.XMPPServiceConnection.onServiceConnected('" +
						 className + "')");
			mXMPPService = ((XMPPService.LocalBinder) binder).getService();
			mXMPPService.registerChatMessageHandler(ChatActivity.this);
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
		Log.d(CLASS, "onCreate(): started chat activity");
		
		setContentView(R.layout.chat);

		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		final String userIDKey = getString(R.string.preference_user_id_key);
		final String userID = settings.getString(userIDKey, "");
		final String serverKey = getString(R.string.preference_server_key);
		final String server = settings.getString(serverKey, "");
		mOwnID = String.format("%s@%s", userID, server);

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
					mXMPPService.sendChatMessage(mSender, sendText);
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
		mXMPPService.unregisterChatMessageHandler(this);
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
		mSender = intent.getStringExtra(XMPPService.SENDER);
		if (mSender != null) {
			int slashIndex = mSender.indexOf('/');
			if (slashIndex != -1) {
				mSender = mSender.substring(0, slashIndex);
			}
			Log.d(CLASS, "chat with " + mSender);
			List<ChatMessage> messages = mDatabase.getMessages(mSender);
			for (ChatMessage message : messages) {
				mListAdapter.add(createMessageSequence(message));
			}
			mListAdapter.notifyDataSetChanged();
			mChatListView.setSelection(mListAdapter.getCount());
		} else {
			final String error = "ChatActivity intent has no sender";
			Log.e(CLASS, error);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(ChatActivity.this, error, Toast.LENGTH_LONG).show();
				}
			});
		}
	}

	@Override
	public boolean handleMessage(final ChatMessage message) {
		Log.d(CLASS, "ChatActivity.handleMessage()");
		boolean handled = false;
		if(message.getTo().startsWith(mSender) || message.getFrom().startsWith(mSender)) {
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
		if (message.getFrom().startsWith(mOwnID)) {
			colour = "green";
		}
		final String from = message.getFrom().substring(0, message.getFrom().indexOf('@'));
		final String chatMessage = String.format("<b><font color=\"%s\">%s:</font></b> %s", colour, from, message.getMessage());
		return Html.fromHtml(chatMessage);
	}
}
