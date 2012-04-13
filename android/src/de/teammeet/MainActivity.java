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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import de.teammeet.helper.ToastDisposerSingleton;
import de.teammeet.service.TeamMeetService;

public class MainActivity extends Activity {

	private String	CLASS	= MainActivity.class.getSimpleName();

	private TeamMeetServiceConnection mServiceConnection = null;

	private ToastDisposerSingleton mToastSingleton = null;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mToastSingleton  = ToastDisposerSingleton.getInstance(getApplicationContext());

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
				sendXMPPMessage();
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		// create the service (if it isn't already running
		final Intent intent = new Intent(getApplicationContext(), TeamMeetService.class);
		startService(intent);

		// now connect to the service
		mServiceConnection  = new TeamMeetServiceConnection();
		final boolean bindSuccess = bindService(intent, mServiceConnection, 0);
		if (bindSuccess) {
			Log.e(CLASS, "bind succeeded");
		} else {
			Log.e(CLASS, "bind failed");
			mToastSingleton.showError("Couldn't connect to service.");
			this.finish();
		}
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

	}
}