package de.teammeet.tasks;

import android.os.AsyncTask;
import android.util.Log;
import de.teammeet.interfaces.AsyncTaskCallback;
import de.teammeet.xmpp.XMPPService;

public class DisconnectTask extends AsyncTask<Void, Void, Void> {

	private static final String CLASS = ConnectTask.class.getSimpleName();

	private XMPPService mService;
	private AsyncTaskCallback<Void> mCallback;

	public DisconnectTask(XMPPService service, AsyncTaskCallback<Void> callback) {
		mService = service;
		mCallback = callback;
	}

	@Override
	protected Void doInBackground(Void... params) {
		mService.disconnect();
		return null;
	}

	@Override
	protected void onPostExecute(Void v) {
		if (mCallback != null) {
			mCallback.onTaskCompleted(v);
		}
		Log.d(CLASS, "Disconnected from XMPP");
	}

}
