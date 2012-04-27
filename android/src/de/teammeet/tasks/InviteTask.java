package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.util.Log;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;

public class InviteTask extends BaseAsyncTask<String, Void, String[]> {

	private static final String CLASS = ConnectTask.class.getSimpleName();

	public InviteTask(IXMPPService service, IAsyncTaskCallback<String[]> callback) {
		super(service, callback);
	}

	@Override
	protected String[] doInBackground(String... params) {

		String contact = params[0];
		String team = params[1];
		String[] conn_data = params;
		
		try {
			mService.invite(contact, team);
		} catch (XMPPException e) {
			Log.e(CLASS, String.format("Failed to invite %s to team %s: %s", contact, team, e.getMessage()));
			mError = e;
			cancel(false);
		}
		
		return conn_data;
	}
}
