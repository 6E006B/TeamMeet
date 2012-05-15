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

package de.teammeet.activities.teams;

import java.util.List;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockMapActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import de.teammeet.R;
import de.teammeet.helper.ActionBarHelper;
import de.teammeet.services.xmpp.XMPPService;

public class TeamMeetActivity extends SherlockMapActivity {

	final String						CLASS						= TeamMeetActivity.class.getSimpleName();

	private MapView						mMapView					= null;
	private List<Overlay>				mListOfOverlays				= null;

	private MyDirectionLocationOverlay	mMyLocationOverlay			= null;
	private MatesOverlay				mMatesOverlay				= null;
	private IndicatorsOverlay			mIndicatorsOverlay			= null;
	private MapGestureDetectorOverlay mMapGestureOverlay = null;

	private String mTeam = null;
	private boolean						mFollowingLocation			= false;
	private boolean						mSatelliteView				= false;
	private boolean						mFullscreen					= false;

	private XMPPService mXMPPService = null;
	private XMPPServiceConnection mXMPPServiceConnection = new XMPPServiceConnection();
	private Intent mCurrentIntent = null;

	private class XMPPServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(CLASS, "TeamMeetActivity.XMPPServiceConnection.onServiceConnected('" + className + "')");
			mXMPPService = ((XMPPService.LocalBinder) binder).getService();

			if (handleIntent(mCurrentIntent)) {
				mMapGestureOverlay.setXMPPService(mXMPPService);
				// register to get status updates
				mXMPPService.startLocationTransmission(mMyLocationOverlay);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			Log.d(CLASS, "TeamMeetActivity.XMPPServiceConnection.onServiceDisconnected('" + className +
					"')");
			mXMPPService = null;
			mMapGestureOverlay.setXMPPService(null);
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapview);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		final String fullscreenKey = getString(R.string.preference_fullscreen_key);
		mFullscreen = settings.getBoolean(fullscreenKey, false);
		final String followLocationKey = getString(R.string.preference_auto_center_key);
		mFollowingLocation = settings.getBoolean(followLocationKey, false);

		mCurrentIntent = getIntent();
	}

	@Override
	protected void onResume() {
		super.onResume();

		Log.d(CLASS, "TeamMeetActivity.onResume()");

		// create the service (if it isn't already running)
		final Intent xmppIntent = new Intent(getApplicationContext(), XMPPService.class);
		startService(xmppIntent);

		Log.d(CLASS, "started location service");

		Window w = getWindow();
		w.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		if (mFullscreen) {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		mMapView = (MapView) findViewById(R.id.mapview);
		Log.d(CLASS, "mMapView:" + mMapView.toString());
		mMapView.setBuiltInZoomControls(true);
		mMapView.setSatellite(mSatelliteView);
		mListOfOverlays = mMapView.getOverlays();

		// now connect to the service
		final boolean bindSuccess = bindService(xmppIntent, mXMPPServiceConnection, 0);
		if (bindSuccess) {
			Log.d(CLASS, "TeamMeetActivity.onResume() bind to XMPP service succeeded");
		} else {
			Log.e(CLASS, "TeamMeetActivity.onResume() bind to XMPP service failed");
			Toast.makeText(getApplicationContext(), "Error: Couldn't connect to XMPP service.",
			               Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		mCurrentIntent = intent;
	}

	@Override
	protected void onPause() {
		if (mMyLocationOverlay != null) {
			mMyLocationOverlay.disableCompass();
		}
		unregisterMatesUpdates();
		if (mXMPPServiceConnection != null) {
			unbindService(mXMPPServiceConnection);
		}
		mXMPPService = null;
		mListOfOverlays.clear();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private void createOverlays(String team) {
		mMatesOverlay = new MatesOverlay(team, this, getResources().getDrawable(R.drawable.matepos),
		                                 mMapView);
		mMapGestureOverlay = new MapGestureDetectorOverlay(mMapView, getApplicationContext());
		mIndicatorsOverlay = new IndicatorsOverlay(team, getApplicationContext(),
		                                           getResources().getDrawable(R.drawable.matepos),
		                                           mMapView);
		mMyLocationOverlay = new MyDirectionLocationOverlay(getApplicationContext(),
		                                                    (MapView) findViewById(R.id.mapview));

		mMyLocationOverlay.enableMyLocation();
		mMyLocationOverlay.enableCompass();
		mMyLocationOverlay.followLocation(mFollowingLocation);
	}

	private void addOverlays() {
		mListOfOverlays.add(mMatesOverlay);
		mListOfOverlays.add(mIndicatorsOverlay);
		mListOfOverlays.add(mMyLocationOverlay);
		mListOfOverlays.add(mMapGestureOverlay);
	}

	private void unregisterMatesUpdates() {
//		if (mMatesOverlay != null && mXMPPService != null) {
//			mXMPPService.unregisterMatesUpdates(mMatesOverlay);
//		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.map, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case android.R.id.home:
				ActionBarHelper.navigateUpInHierarchy(this);
				return true;
		
			case R.id.goto_mylocation:
				focusCurrentLocation();
				return true;

			case R.id.auto_center:
				toggleFollowingLocation();
				return true;

			case R.id.satellite_view:
				toggleSatelliteView();
				return true;

			case R.id.fullscreen:
				toggleFullscreen();
				return true;
				
			default:
				return super.onOptionsItemSelected(item);
		}
		
	}

	private boolean handleIntent(Intent intent) {
		boolean success = true;
		mTeam = intent.getStringExtra(XMPPService.GROUP);
		if (mTeam != null) {
			createOverlays(mTeam);
			addOverlays();
		} else {
			success = false;
			final String error = "Intent had no team!";
			Log.e(CLASS, error);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
					finish();
				}
			});
		}
		return success;
	}

	private void toggleFollowingLocation() {
		mFollowingLocation = !mFollowingLocation;
		mMyLocationOverlay.followLocation(mFollowingLocation);
	}

	private void toggleSatelliteView() {
		mSatelliteView = !mSatelliteView;
		mMapView.setSatellite(mSatelliteView);
	}

	private void focusCurrentLocation() {
		mMyLocationOverlay.focusCurrentLocation();
	}

	private void toggleFullscreen() {
		mFullscreen = !mFullscreen;
		if (mFullscreen) {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
	}
}