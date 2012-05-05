package de.teammeet.activities.chat;

import java.util.List;

import org.jivesoftware.smack.XMPPException;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import de.teammeet.R;
import de.teammeet.services.xmpp.ChatMessage;
import de.teammeet.services.xmpp.XMPPService;

public class ChatFragment extends Fragment {
	
	private static final String CLASS = ChatFragment.class.getSimpleName();
	
	private ListView mChatListView = null;
	private ArrayAdapter<CharSequence> mListAdapter = null;
	private EditText mChatEditText = null;
	private int mType = 0;
	private String mCounterpart = null;
	private Chat mChat = null;
	public Intent mCurrentIntent = null;

	private XMPPService mXMPPService = null;
	private XMPPServiceConnection mXMPPServiceConnection = new XMPPServiceConnection();


	private class XMPPServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(CLASS, "ChatFragment.XMPPServiceConnection.onServiceConnected('" +
						 className + "')");
			mXMPPService = ((XMPPService.LocalBinder) binder).getService();

			mChatEditText.setEnabled(true);

			mXMPPService.registerChatMessageHandler(mChat);
			mXMPPService.registerGroupMessageHandler(mChat);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			Log.d(CLASS, "ChatFragment.XMPPServiceConnection.onServiceDisconnected('" +
						 className + "')");
			mXMPPService = null;
		}
	};

	public static ChatFragment getInstance(int type, String counterpart) {
		ChatFragment f = new ChatFragment();

		Bundle args = new Bundle();
        args.putInt(XMPPService.TYPE, type);
        args.putString(XMPPService.SENDER, counterpart);
        f.setArguments(args);

        return f;
	}

	public ChatFragment() {}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(CLASS, "ChatFragment.onCreateView()");
		if (container == null) {
            // We have different layouts, and in one of them this
            // fragment's containing frame doesn't exist.  The fragment
            // may still be created from its saved state, but there is
            // no reason to try to create its view hierarchy because it
            // won't be displayed.  Note this is not needed -- we could
            // just run the code below, where we would create and return
            // the view hierarchy; it would just never be used.
            return null;
        }

		mType = getArguments().getInt(XMPPService.TYPE);
        mCounterpart = getArguments().getString(XMPPService.SENDER);

        mChat = new Chat(mType, mCounterpart, this);

		LinearLayout rootView = (LinearLayout) inflater.inflate(R.layout.chat, container, false);
		mChatListView = (ListView) rootView.findViewById(R.id.chatListView);
		mListAdapter = new ArrayAdapter<CharSequence>(getActivity(), R.layout.chat_item);
		mChatListView.setAdapter(mListAdapter);
		mChatEditText = (EditText) rootView.findViewById(R.id.chatInput);
		if (mXMPPService == null) {
			mChatEditText.setEnabled(false);
		}
		mChatEditText.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				String sendText = v.getText().toString();
				try {
					mChat.sendMessage(sendText, mXMPPService);
					v.setText("");
				} catch (XMPPException e) {
					final String errorMessage = "Unable to send message:\n" + e.getMessage();
					Log.e(CLASS, errorMessage);
					e.printStackTrace();
					mChatEditText.post(new Runnable() {
						@Override
						public void run() {
							final Toast toast =
									Toast.makeText(getActivity().getApplicationContext(),
									               errorMessage, Toast.LENGTH_LONG);
							toast.setGravity(Gravity.BOTTOM, 0, 0);
							toast.show();
						}
					});
				}
				return true;
			}
		});
		List<ChatMessage> messages = mChat.fetchMessages();
		for (ChatMessage message : messages) {
			mListAdapter.add(mChat.createMessageSequence(message));
		}
		mListAdapter.notifyDataSetChanged();
		mChatListView.setSelection(mListAdapter.getCount());
		return rootView;
	}
	
	@Override
	public void onResume() {
		Log.d(CLASS, "ChatFragment.onResume()");
		super.onResume();

		// create the service (if it isn't already running)
		final Intent xmppIntent = new Intent(getActivity().getApplicationContext(),
		                                     XMPPService.class);
		getActivity().startService(xmppIntent);

		Log.d(CLASS, "started XMPP service");

		// now connect to the service
		boolean bindSuccess = getActivity().bindService(xmppIntent, mXMPPServiceConnection, 0);
		if (bindSuccess) {
			Log.d(CLASS, "onResume(): bind to XMPP service succeeded");
		} else {
			Log.e(CLASS, "onResume(): bind to XMPP service failed");
			Toast.makeText(getActivity().getApplicationContext(),
			               "Couldn't connect to XMPP service.", 3);
		}
	}

	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(CLASS, "ChatFragment.onSaveInstanceState()");
        outState.putInt("lastState", mListAdapter.getCount());
    }

	@Override
	public void onPause() {
		Log.d(CLASS, "ChatFragment.onPause()");
		if (mXMPPService != null) {
			mXMPPService.unregisterChatMessageHandler(mChat);
			mXMPPService.unregisterGroupMessageHandler(mChat);
		}
		if (mXMPPServiceConnection != null) {
			getActivity().unbindService(mXMPPServiceConnection);
		}
		mXMPPService = null;
		super.onPause();
	}

	@Override
	public void onStop() {
		Log.d(CLASS, "ChatFragment.onStop()");
		super.onStop();
	}

	@Override
	public void onDetach() {
		Log.d(CLASS, "ChatFragment.onDetach()");
		super.onDetach();
	}

	@Override
	public void onDestroy() {
		Log.d(CLASS, "ChatFragment.onDestroy()");
		super.onDestroy();
	}

	public void handleMessage(final CharSequence message) {
		Log.d(CLASS, String.format("ChatFragment.handleMessage('%s')", message));
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
