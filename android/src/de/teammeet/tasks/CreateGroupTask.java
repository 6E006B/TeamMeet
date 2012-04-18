package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;
import de.teammeet.xmpp.XMPPService;

public class CreateGroupTask extends AsyncTask<String, Void, Boolean> {

	private static final String CLASS = ConnectTask.class.getSimpleName();

	private XMPPService mService;
	private Button mCreateButton;

	public CreateGroupTask(XMPPService service, Button button) {
		assert mService != null : "Cannot create group without a service";
		mService = service;
		mCreateButton = button;
	}

	@Override
	protected Boolean doInBackground(String... params) {

		String groupName = params[0];
		String conferenceServer = params[1];
		Boolean success = true;

		try {
			mService.createGroup(groupName, conferenceServer);
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
			mCreateButton.setText("Joined!");
		}
	}

}
