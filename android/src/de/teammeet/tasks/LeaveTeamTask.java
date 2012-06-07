package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.util.Log;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;

public class LeaveTeamTask extends BaseAsyncTask<String, Void, String> {

	private static final String CLASS = LeaveTeamTask.class.getSimpleName();

	public LeaveTeamTask(IXMPPService service, IAsyncTaskCallback<String> callback) {
		super(service, callback);
	}

	@Override
	protected String doInBackground(String... params) {

		String team = params[0];

		try {
			mService.leaveTeam(team);
		} catch (XMPPException e) {
			Log.e(CLASS, String.format("Failed to leave team '%s': %s", team, e.getMessage()));
			mError = e;
			cancel(false);
		}

		return team;
	}
}
