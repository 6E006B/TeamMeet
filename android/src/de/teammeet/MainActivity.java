/**
 *    Copyright 2012 Daniel Kreischer, Christopher Holm, Christopher Schwardt
 *
 *    This file is part of TeamMeet.
 *
 *    TeamMeet is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    TeamMeet is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with TeamMeet.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.teammeet;

import org.jivesoftware.smack.XMPPException;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import de.teammeet.helper.ToastDisposerSingleton;
import de.teammeet.xmpp.XMPPService;

public class MainActivity extends Activity {

	private String					CLASS				= MainActivity.class.getSimpleName();

	private ServiceConnection		mServiceConnection	= new ServiceConnection() {
															@Override
															public void onServiceConnected(
																	ComponentName className, IBinder binder) {
																Log.d(CLASS,
																		"MainActivity.ServiceConnection.onServiceConnected('" +
																				className + "')");
																mXMPPService = ((XMPPService.LocalBinder) binder)
																		.getService();
															}

															@Override
															public void onServiceDisconnected(
																	ComponentName className) {
																Log.d(CLASS,
																		"MainActivity.ServiceConnection.onServiceDisconnected('" +
																				className + "')");
															}
														};

	private XMPPService				mXMPPService		= null;

	private ToastDisposerSingleton	mToastSingleton		= null;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mToastSingleton = ToastDisposerSingleton.getInstance(getApplicationContext());

		Button b;

		b = (Button) findViewById(R.id.buttonShowMap);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View arg0) {
				startMapActivity();
			}
		});

		b = (Button) findViewById(R.id.buttonShowSettings);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View arg0) {
				startSettingsActivity();
			}
		});

		b = (Button) findViewById(R.id.buttonXMPP);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						sendXMPPMessage();
					}
				}).start();
			}
		});

		// If the user has not yet configured his XMPP settings lead the way
		SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
		if (settings.getString(SettingsActivity.SETTING_XMPP_USER_ID, "").equals("") ||
				settings.getString(SettingsActivity.SETTING_XMPP_SERVER, "").equals("") ||
				settings.getString(SettingsActivity.SETTING_XMPP_PASSWORD, "").equals("")) {
			mToastSingleton.show("Please configure your XMPP Account.");
			Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
			startActivity(settingsIntent);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		// create the service (if it isn't already running
		final Intent intent = new Intent(getApplicationContext(), XMPPService.class);
		startService(intent);

		// now connect to the service
		final boolean bindSuccess = bindService(intent, mServiceConnection, 0);
		if (bindSuccess) {
			Log.e(CLASS, "bind succeeded");
		} else {
			Log.e(CLASS, "bind failed");
			mToastSingleton.showError("Couldn't connect to service.");
			this.finish();
		}
	}

	@Override
	protected void onPause() {
		if (mServiceConnection != null) {
			unbindService(mServiceConnection);
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		mXMPPService.disconnect();
		final Intent intent = new Intent(getApplicationContext(), XMPPService.class);
		stopService(intent);
		super.onDestroy();
	}

	protected void startMapActivity() {
		final Intent intent = new Intent(MainActivity.this, TeamMeetActivity.class);
		startActivity(intent);
	}

	protected void startSettingsActivity() {
		final Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
		startActivity(intent);
	}

	protected void sendXMPPMessage() {
		Log.d(CLASS, "MainActivity.sendXMPPMessage()");

		if (!mXMPPService.isAuthenticated()) {
			SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
			try {
				mXMPPService.connect(settings.getString(SettingsActivity.SETTING_XMPP_USER_ID, ""),
						settings.getString(SettingsActivity.SETTING_XMPP_SERVER, ""),
						settings.getString(SettingsActivity.SETTING_XMPP_PASSWORD, ""));
			} catch (XMPPException e) {
				e.printStackTrace();
				Log.e(CLASS, "Failed to login: " + e.toString());
				mToastSingleton.showError("Failed to login: " + e.toString());
			}
		} else {
			mXMPPService.disconnect();
			Log.d(CLASS, "Disconnected from XMPP");
		}
	}
}