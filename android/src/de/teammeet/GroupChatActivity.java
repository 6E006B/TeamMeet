package de.teammeet;

import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.teammeet.helper.GroupChatOpenHelper;
import de.teammeet.interfaces.IGroupMessageHandler;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.xmpp.GroupChatMessage;
import de.teammeet.xmpp.XMPPService;

public class GroupChatActivity extends Activity implements IGroupMessageHandler {

	private static final String CLASS = GroupChatActivity.class.getSimpleName();

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
		
		mChatTextView = (TextView)findViewById(R.id.chatTextView);
		mChatEditText = (EditText)findViewById(R.id.chatInput);
		mChatEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				String text = s.toString();
				int index = text.indexOf("\n");
				if (index != -1) {
					String sendText = text.substring(0, index);
					String spareText = text.substring(index, text.length());
//					mXMPPService.sendGroupMessage(mGroup, sendText);
					s.clear();
					s.insert(0, spareText);
				}
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
				chatText += String.format("%s: %s", message.getFrom(), message.getMessage());
			}
			mChatTextView.setText(chatText);
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
		// TODO Auto-generated method stub
		return false;
	}
}
