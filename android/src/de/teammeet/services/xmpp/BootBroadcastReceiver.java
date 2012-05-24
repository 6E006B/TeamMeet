package de.teammeet.services.xmpp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.teammeet.R;

public class BootBroadcastReceiver extends BroadcastReceiver {

	private static final String CLASS = BootBroadcastReceiver.class.getSimpleName();
	private static final String BOOT_ACTION = "android.intent.action.BOOT_COMPLETED";

	@Override
	public void onReceive(Context context, Intent intent) {
		//All registered broadcasts are received by this
		String action = intent.getAction();
		if (action.equalsIgnoreCase(BOOT_ACTION)) {
			//check for boot complete event & start your service
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
			if (settings.getBoolean(context.getString(R.string.preference_autostart_key), false)) {
				Log.d(CLASS, "Autostarting XMPPService");
				startXMPPService(context);
			} else {
				Log.d(CLASS, "Not autostarting XMPPService");
			}
		}
	}

	private void startXMPPService(Context context) {
		Intent serviceIntent = new Intent(context, XMPPService.class);
		serviceIntent.putExtra(XMPPService.ACTION, XMPPService.ACTION_CONNECT);
		context.startService(serviceIntent);
	}
}
