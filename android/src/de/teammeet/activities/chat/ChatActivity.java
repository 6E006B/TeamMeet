package de.teammeet.activities.chat;

import java.util.List;

import org.jivesoftware.smack.XMPPException;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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
import de.teammeet.services.xmpp.ChatMessage;
import de.teammeet.services.xmpp.XMPPService;

public class ChatActivity extends Activity {

	private static final String CLASS = ChatActivity.class.getSimpleName();

	private ListView mChatListView = null;
	private ArrayAdapter<CharSequence> mListAdapter = null;
	private EditText mChatEditText = null;
	private Chat mChat = null;
	public Intent mCurrentIntent = null;

	private XMPPService mXMPPService = null;
	private XMPPServiceConnection mXMPPServiceConnection = new XMPPServiceConnection();


	private class XMPPServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(CLASS, "RosterActivity.XMPPServiceConnection.onServiceConnected('" +
						 className + "')");
			mXMPPService = ((XMPPService.LocalBinder) binder).getService();

			Log.d(CLASS, "ChatActivity: " + getIntent().getIntExtra(XMPPService.TYPE, 0));
			Log.d(CLASS, "ChatActivity: " + getIntent().getStringExtra(XMPPService.SENDER));
			handleIntent(mCurrentIntent);

			mXMPPService.registerChatMessageHandler(mChat);
			mXMPPService.registerGroupMessageHandler(mChat);
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

		mChatListView = (ListView)findViewById(R.id.chatListView);
		mListAdapter = new ArrayAdapter<CharSequence>(this, R.layout.chat_item);
		mChatListView.setAdapter(mListAdapter);
		mChatEditText = (EditText)findViewById(R.id.chatInput);

		mCurrentIntent = getIntent();

		mChatEditText.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				String sendText = v.getText().toString();
				try {
					mChat.sendMessage(sendText);
					v.setText("");
				} catch (XMPPException e) {
					final String errorMessage = "Unable to send message:\n" + e.getMessage();
					Log.e(CLASS, errorMessage);
					e.printStackTrace();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							final Toast toast = Toast.makeText(ChatActivity.this, errorMessage,
							                                   Toast.LENGTH_LONG);
							toast.setGravity(Gravity.BOTTOM, 0, 0);
							toast.show();
						}
					});
				}
				return true;
			}
		});
	}

	@Override
	protected void onResume() {
		Log.d(CLASS, "ChatActivity.onResume()");
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
		Log.d(CLASS, "ChatActivity.onPause()");
		mXMPPService.unregisterChatMessageHandler(mChat);
		mXMPPService.unregisterGroupMessageHandler(mChat);
		if (mXMPPServiceConnection != null) {
			unbindService(mXMPPServiceConnection);
		}
		mXMPPService = null;
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		Log.d(CLASS, "ChatActivity.onDestroy()");
		super.onDestroy();
	}
	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(CLASS, "ChatActivity.onNewIntent()");
		super.onNewIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		mChat = new Chat(intent, mXMPPService, this);
		List<ChatMessage> messages = mChat.fetchMessages();
		for (ChatMessage message : messages) {
			mListAdapter.add(mChat.createMessageSequence(message));
		}
		mListAdapter.notifyDataSetChanged();
		mChatListView.setSelection(mListAdapter.getCount());
	}

	public void handleMessage(final CharSequence message) {
		Log.d(CLASS, String.format("ChatActivity.handleMessage('%s')", message));
		mChatListView.post(new Runnable() {
			@Override
			public void run() {
				mListAdapter.add(message);
				mListAdapter.notifyDataSetChanged();
				Log.d(CLASS, "list adapter count is "+mListAdapter.getCount());
				mChatListView.setSelection(mListAdapter.getCount());
			}
		});
	}
}
