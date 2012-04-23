package de.teammeet.tasks;

import android.os.AsyncTask;
import de.teammeet.interfaces.AsyncTaskCallback;
import de.teammeet.xmpp.XMPPService;

public class InviteTask extends AsyncTask<String, Void, Void> {

	// private static final String CLASS = ConnectTask.class.getSimpleName();

	private XMPPService mService;
	private AsyncTaskCallback<Void> mCallback;

	public InviteTask(XMPPService service, AsyncTaskCallback<Void> callback) {
		assert mService != null : "Cannot create group without a service";
		mService = service;
		mCallback = callback;
	}

	@Override
	protected Void doInBackground(String... params) {

		String contact = params[0];
		String groupName = params[1];

		mService.invite(contact, groupName);

		return null;
	}

	@Override
	protected void onPostExecute(Void v) {
		mCallback.onTaskCompleted(v);
	}

}
