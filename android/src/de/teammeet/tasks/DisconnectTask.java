package de.teammeet.tasks;

import android.os.AsyncTask;
import android.util.Log;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.xmpp.XMPPService;

public class DisconnectTask extends AsyncTask<Void, Void, Void> {

	private static final String CLASS = ConnectTask.class.getSimpleName();

	private XMPPService mService;
	private IAsyncTaskCallback<Void> mCallback;

	public DisconnectTask(XMPPService service, IAsyncTaskCallback<Void> callback) {
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
