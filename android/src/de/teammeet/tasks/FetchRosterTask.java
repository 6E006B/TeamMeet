package de.teammeet.tasks;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPException;

import android.util.Log;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;

public class FetchRosterTask extends BaseAsyncTask<Void, Void, Roster> {

	private static final String CLASS = FetchRosterTask.class.getSimpleName();

	public FetchRosterTask(IXMPPService service, IAsyncTaskCallback<Roster> callback) {
		super(service, callback);
	}

	@Override
	protected Roster doInBackground(Void... params) {

		Roster roster = null;
		
		try {
			roster = mService.getRoster();
		} catch (XMPPException e) {
			Log.e(CLASS, String.format("Could not fetch roster: %s", e.getMessage()));
			mError = e;
			cancel(false);
		}

		return roster;
	}
}
