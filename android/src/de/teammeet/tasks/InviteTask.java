package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.os.AsyncTask;
import android.util.Log;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;

public class InviteTask extends AsyncTask<String, Void, String[]> {

	private static final String CLASS = ConnectTask.class.getSimpleName();

	private IXMPPService mService;
	private IAsyncTaskCallback<String[]> mCallback;

	public InviteTask(IXMPPService service, IAsyncTaskCallback<String[]> callback) {
		assert mService != null : "Cannot create group without a service";
		mService = service;
		mCallback = callback;
	}

	@Override
	protected String[] doInBackground(String... params) {

		String contact = params[0];
		String team = params[1];
		String[] conn_data = params;
		
		try {
			mService.invite(contact, team);
		} catch (XMPPException e) {
			conn_data = new String[0];
			Log.e(CLASS, String.format("Failed to invite %s to team %s: %s", contact, team, e.getMessage()));
		}
		
		return conn_data;
	}

	@Override
	protected void onPostExecute(String[] connection_data) {
		mCallback.onTaskCompleted(connection_data);
	}

}
