package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import de.teammeet.SettingsActivity;
import de.teammeet.interfaces.AsyncTaskCallback;
import de.teammeet.xmpp.XMPPService;

public class ConnectTask extends AsyncTask<Void, Void, Boolean> {

	private static final String CLASS = ConnectTask.class.getSimpleName();

	private XMPPService mService;
	private AsyncTaskCallback mCallback;

	public ConnectTask(XMPPService service, AsyncTaskCallback callback) {
		mService = service;
		mCallback = callback;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		Boolean success = true;

		SharedPreferences settings = mService.getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
		try {
			String user = settings.getString(SettingsActivity.SETTING_XMPP_USER_ID, "");
			String server = settings.getString(SettingsActivity.SETTING_XMPP_SERVER, "");
			String passwd = settings.getString(SettingsActivity.SETTING_XMPP_PASSWORD, "");
			mService.connect(user, server, passwd);
		} catch (XMPPException e) {
			success = false;
			e.printStackTrace();
			Log.e(CLASS, "Failed to login: " + e.toString());
		}

		return success;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (result) {
			if (mCallback != null) {
				mCallback.onTaskCompleted();
			}
			Log.d(CLASS, "successfully logged in");
		} else {
			Log.d(CLASS, "login failed");
		}
	}

}
