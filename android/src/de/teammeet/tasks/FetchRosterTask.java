package de.teammeet.tasks;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPException;

import android.os.AsyncTask;
import android.util.Log;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;

public class FetchRosterTask extends AsyncTask<Void, Void, Roster> {

	private static final String CLASS = FetchRosterTask.class.getSimpleName();

	private IXMPPService mService;
	private IAsyncTaskCallback<Roster> mCallback;
	private Exception mError;

	public FetchRosterTask(IXMPPService service, IAsyncTaskCallback<Roster> callback) {
		mService = service;
		mCallback = callback;
	}

	@Override
	protected Roster doInBackground(Void... params) {

		Roster roster = null;
		
		try {
			roster = mService.getRoster();
		} catch (XMPPException e) {
			Log.e(CLASS, "Could not fetch Roster: " + e.toString());
			mError = e;
			cancel(false);
		}

		return roster;
	}

	@Override
	protected void onPostExecute(Roster roster) {
		mCallback.onTaskCompleted(roster);
	}
	
	@Override
	protected void onCancelled() {
		mCallback.onTaskAborted(mError);
	}
}
