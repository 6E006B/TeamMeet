package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.os.AsyncTask;
import android.util.Log;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;

public class CreateGroupTask extends AsyncTask<String, Void, String[]> {

	private static final String CLASS = ConnectTask.class.getSimpleName();

	private IXMPPService mService;
	private IAsyncTaskCallback<String[]> mCallback;

	public CreateGroupTask(IXMPPService mXMPPService, IAsyncTaskCallback<String[]> callback) {
		assert mService != null : "Cannot create group without a service";
		mService = mXMPPService;
		mCallback = callback;
	}

	@Override
	protected String[] doInBackground(String... params) {

		String groupName = params[0];
		String conferenceServer = params[1];
		String[] conn_data = params;

		try {
			mService.createRoom(groupName, conferenceServer);
		} catch (XMPPException e) {
			conn_data = new String[0];
			e.printStackTrace();
			Log.e(CLASS, String.format("Failed to create group '%s': %s", groupName, e.toString()));
		}

		return conn_data;
	}

	@Override
	protected void onPostExecute(String[] connection_data) {
		mCallback.onTaskCompleted(connection_data);
	}

}
