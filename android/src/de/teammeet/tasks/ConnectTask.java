package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.content.SharedPreferences;
import android.util.Log;
import de.teammeet.activities.preferences.SettingsActivity;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.services.xmpp.XMPPService;

public class ConnectTask extends BaseAsyncTask<Void, Void, Void> {

	private static final String CLASS = ConnectTask.class.getSimpleName();

	public ConnectTask(IXMPPService service, IAsyncTaskCallback<Void> callback) {
		super(service, callback);
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		SharedPreferences settings = ((XMPPService) mService).getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
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
}
