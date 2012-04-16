package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;
import de.teammeet.SettingsActivity;
import de.teammeet.xmpp.XMPPService;



public class ConnectTask extends AsyncTask<XMPPService, Void, Boolean>
{
	private static final String CLASS = ConnectTask.class.getSimpleName();
	private Button mconnectButton;
	
	public ConnectTask(Button button) {
		mconnectButton = button;
	}
	
	@Override
	protected Boolean doInBackground(XMPPService... params) {
		XMPPService service = params[0];
		Boolean success = true;
		
		SharedPreferences settings = service.getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
		try {
			service.connect(settings.getString(SettingsActivity.SETTING_XMPP_USER_ID, ""),
					settings.getString(SettingsActivity.SETTING_XMPP_SERVER, ""),
					settings.getString(SettingsActivity.SETTING_XMPP_PASSWORD, ""));
		} catch (XMPPException e) {
			success = false;
			e.printStackTrace();
			Log.e(CLASS, "Failed to login: " + e.toString());
			//mToastSingleton.showError("Failed to login: " + e.toString());
		}
		
		return success;
	}

	@Override
	protected void onPostExecute (Boolean result) {
		if (result) {
			mconnectButton.setText("disconnect");
			Log.d(CLASS, "successfully logged in");
		} else {
			Log.d(CLASS, "login failed");
		}
	}
	
}
