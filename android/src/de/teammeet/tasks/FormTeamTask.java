package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.util.Log;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;

public class FormTeamTask extends BaseAsyncTask<String, Void, String> {

	private static final String CLASS = ConnectTask.class.getSimpleName();

	public FormTeamTask(IXMPPService service, IAsyncTaskCallback<String> callback) {
		super(service, callback);
	}

	@Override
	protected String doInBackground(String... params) {

		String groupName = params[0];
		String conferenceServer = params[1];

		try {
			mService.createRoom(groupName, conferenceServer);
		} catch (XMPPException e) {
			Log.e(CLASS, String.format("Failed to create group '%s': %s", groupName, e.toString()), e);
			mError = e;
			cancel(false);
		}

		return groupName;
	}
}
