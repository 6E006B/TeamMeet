package de.teammeet.tasks;

import java.util.Set;

import org.jivesoftware.smack.XMPPException;

import android.util.Log;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;

public class FetchTeamsTask extends BaseAsyncTask<Void, Void, Set<String>> {

	private static final String CLASS = FetchTeamsTask.class.getSimpleName();

	public FetchTeamsTask(IXMPPService service, IAsyncTaskCallback<Set<String>> callback) {
		super(service, callback);
	}

	@Override
	protected Set<String> doInBackground(Void... params) {

		Set<String> teams = null;

		try {
			teams =  mService.getTeams();
		} catch (XMPPException e) {
			Log.e(CLASS, String.format("Could not fetch teams: %s", e.getMessage()));
			mError = e;
			cancel(false);
		}

		return teams;
	}
}
