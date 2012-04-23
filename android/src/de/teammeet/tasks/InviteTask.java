package de.teammeet.tasks;

import android.os.AsyncTask;
import de.teammeet.interfaces.AsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;

public class InviteTask extends AsyncTask<String, Void, String[]> {

	// private static final String CLASS = ConnectTask.class.getSimpleName();

	private IXMPPService mService;
	private AsyncTaskCallback<String[]> mCallback;

	public InviteTask(IXMPPService service, AsyncTaskCallback<String[]> callback) {
		assert mService != null : "Cannot create group without a service";
		mService = service;
		mCallback = callback;
	}

	@Override
	protected String[] doInBackground(String... params) {

		String contact = params[0];
		String groupName = params[1];

		mService.invite(contact, groupName);

		return params;
	}

	@Override
	protected void onPostExecute(String[] params) {
		mCallback.onTaskCompleted(params);
	}

}
