package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;
import de.teammeet.SettingsActivity;
import de.teammeet.xmpp.XMPPService;


public class ConnectTask extends AsyncTask<Void, Void, Boolean>
{
	private static final String CLASS = ConnectTask.class.getSimpleName();
	private XMPPService mService;
	private Button mConnectButton;
	
	
	public ConnectTask(XMPPService service, Button button) {
		mService = service;
		mConnectButton = button;
	}
	
	@Override
	protected Boolean doInBackground(Void... params) {
		Boolean success = true;
		
		SharedPreferences settings = mService.getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
		try {
			mService.connect(settings.getString(SettingsActivity.SETTING_XMPP_USER_ID, ""),
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
			mConnectButton.setText("disconnect");
			Log.d(CLASS, "successfully logged in");
		} else {
			Log.d(CLASS, "login failed");
		}
	}

}
