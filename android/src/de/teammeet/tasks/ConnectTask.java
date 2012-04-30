package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.teammeet.R;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.xmpp.XMPPService;

public class ConnectTask extends BaseAsyncTask<Void, Void, Void> {

	private static final String CLASS = ConnectTask.class.getSimpleName();

	public ConnectTask(IXMPPService service, IAsyncTaskCallback<Void> callback) {
		super(service, callback);
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		XMPPService xmppService = (XMPPService)mService;
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(xmppService);
		try {
			String user =
					settings.getString(xmppService.getString(R.string.preference_user_id_key), "");
			String server =
					settings.getString(xmppService.getString(R.string.preference_server_key), "");
			String passwd =
					settings.getString(xmppService.getString(R.string.preference_password_key), "");
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
