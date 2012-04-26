package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import de.teammeet.SettingsActivity;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.xmpp.XMPPService;

public class ConnectTask extends AsyncTask<Void, Void, Void> {

	private static final String CLASS = ConnectTask.class.getSimpleName();

	private XMPPService mService;
	private IAsyncTaskCallback<Void> mCallback;
	private Exception mError;

	public ConnectTask(XMPPService service, IAsyncTaskCallback<Void> callback) {
		mService = service;
		mCallback = callback;
	}

	@Override
	protected Void doInBackground(Void... params) {
		SharedPreferences settings = mService.getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
		try {
			String user = settings.getString(SettingsActivity.SETTING_XMPP_USER_ID, "");
			String server = settings.getString(SettingsActivity.SETTING_XMPP_SERVER, "");
			String passwd = settings.getString(SettingsActivity.SETTING_XMPP_PASSWORD, "");
			mService.connect(user, server, passwd);
		} catch (XMPPException e) {
			Log.e(CLASS, "Failed to login: " + e.toString());
			e.printStackTrace();
			mError = e;
			cancel(false);
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void nothing) {
		Log.d(CLASS, "Successfully logged in");
		if (mCallback != null) {
			mCallback.onTaskCompleted(nothing);
		}
	}

	@Override
	protected void onCancelled() {
		Log.d(CLASS, "connect task was cancelled!");
		if (mCallback != null) {
			mCallback.onTaskAborted(mError);
		}
	}

}
