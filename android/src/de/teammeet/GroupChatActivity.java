package de.teammeet;

import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import de.teammeet.helper.GroupChatOpenHelper;
import de.teammeet.interfaces.IGroupMessageHandler;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.xmpp.GroupChatMessage;
import de.teammeet.xmpp.XMPPService;

public class GroupChatActivity extends Activity implements IGroupMessageHandler {

	private static final String CLASS = GroupChatActivity.class.getSimpleName();

	private ScrollView mScrollView = null;
	private TextView mChatTextView = null;
	private EditText mChatEditText = null;
	private String mGroup = null;
	private GroupChatOpenHelper mDatabase = null;

	private IXMPPService mXMPPService = null;
	private XMPPServiceConnection mXMPPServiceConnection = new XMPPServiceConnection();

	private class XMPPServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(CLASS, "RosterActivity.XMPPServiceConnection.onServiceConnected('" + className + "')");
			mXMPPService = ((XMPPService.LocalBinder) binder).getService();
			mXMPPService.registerGroupMessageHandler(GroupChatActivity.this);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			Log.d(CLASS, "RosterActivity.XMPPServiceConnection.onServiceDisconnected('" + className + "')");
			mXMPPService = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS, "onCreate(): started group chat");
		
		setContentView(R.layout.groupchat);

		mScrollView = (ScrollView)findViewById(R.id.scrollView);
		mChatTextView = (TextView)findViewById(R.id.chatTextView);
		mChatEditText = (EditText)findViewById(R.id.chatInput);
		mChatEditText.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				String sendText = v.getText().toString();
				Log.d(CLASS, "sending: " + sendText);
//				mXMPPService.sendGroupMessage(mGroup, sendText);
				v.setText("");
				return false;
			}
		});

		mDatabase = new GroupChatOpenHelper(getApplicationContext());
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
		handleIntent(getIntent());
	}

	@Override
	protected void onPause() {
		if (mXMPPServiceConnection != null) {
			unbindService(mXMPPServiceConnection);
		}
		mXMPPService = null;
		super.onPause();
	}

	private void handleIntent(Intent intent) {
		mGroup = intent.getStringExtra(XMPPService.GROUP);
		if (mGroup != null) {
			String chatText = "";
			List<GroupChatMessage> messages = mDatabase.getMessages(mGroup);
			for (GroupChatMessage message : messages) {
				final String from = message.getFrom().split("@", 2)[0];
				chatText += String.format("%s: %s\n", from, message.getMessage());
			}
			mChatTextView.setText(chatText);
			mScrollView.post(new Runnable() {
				@Override
				public void run() {
					mScrollView.smoothScrollTo(0, mChatTextView.getBottom());
				}
			});
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
	public boolean handleGroupMessage(GroupChatMessage message) {
		Log.d(CLASS, "GroupChatActivity.handleGroupMessage()");
		if (message.getGroup().equals(mGroup)) {
			final String from = message.getFrom().split("@", 2)[0];
			final String chatText = String.format("%s: %s\n", from, message.getMessage());
			mScrollView.post(new Runnable() {
				@Override
				public void run() {
					mChatTextView.append(chatText);
					mChatTextView.post(new Runnable() {
						@Override
						public void run() {
							mScrollView.smoothScrollTo(0, mChatTextView.getBottom());
						}
					});
				}
			});
		}
		return false;
	}
}
