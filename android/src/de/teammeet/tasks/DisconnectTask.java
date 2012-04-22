package de.teammeet.tasks;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;
import de.teammeet.xmpp.XMPPService;

public class DisconnectTask extends AsyncTask<Void, Void, Void> {

	private static final String CLASS = ConnectTask.class.getSimpleName();

	private XMPPService mService;
	private Button mDisconnectButton;

	public DisconnectTask(XMPPService service, Button button) {
		mService = service;
		mDisconnectButton = button;
	}

	@Override
	protected Void doInBackground(Void... params) {
		mService.disconnect();
		return null;
	}

	@Override
	protected void onPostExecute(Void v) {
		if (mDisconnectButton != null) {
			mDisconnectButton.setText("Connect");
		}
		Log.d(CLASS, "Disconnected from XMPP");
	}

}
