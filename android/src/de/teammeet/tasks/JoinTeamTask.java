package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.util.Log;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;

public class JoinTeamTask extends BaseAsyncTask<String, Void, String> {

	private static final String CLASS = ConnectTask.class.getSimpleName();

	public JoinTeamTask(IXMPPService service, IAsyncTaskCallback<String> callback) {
		super(service, callback);
	}

	@Override
	protected String doInBackground(String... params) {

		String team = params[0];
		String userID = params[1];
		String password = params[2];
		String inviter = params[3];

		try {
			mService.joinTeam(team, userID, password, inviter);
		} catch (XMPPException e) {
			Log.e(CLASS, String.format("Failed to join team '%s': %s", team, e.toString()), e);
			mError = e;
			cancel(false);
		}

		return team;
	}
}
