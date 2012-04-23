package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.os.AsyncTask;
import android.util.Log;
import de.teammeet.interfaces.AsyncTaskCallback;
import de.teammeet.xmpp.XMPPService;

public class CreateGroupTask extends AsyncTask<String, Void, Boolean> {

	private static final String CLASS = ConnectTask.class.getSimpleName();

	private XMPPService mService;
	private AsyncTaskCallback<Boolean> mCallback;

	public CreateGroupTask(XMPPService service, AsyncTaskCallback<Boolean> callback) {
		assert mService != null : "Cannot create group without a service";
		mService = service;
		mCallback = callback;
	}

	@Override
	protected Boolean doInBackground(String... params) {

		String groupName = params[0];
		String conferenceServer = params[1];
		Boolean success = true;

		try {
			mService.createRoom(groupName, conferenceServer);
		} catch (XMPPException e) {
			success = false;
			e.printStackTrace();
			Log.e(CLASS, String.format("Failed to create group '%s': %s", groupName, e.toString()));
		}

		return success;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (result) {
			mCallback.onTaskCompleted(result);
		}
	}

}
