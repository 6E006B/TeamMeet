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

import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import de.teammeet.helper.LocationFollower;
import de.teammeet.helper.ToastDisposerSingleton;
import de.teammeet.service.TeamMeetService;

public class TeamMeetActivity extends MapActivity {

	final String						CLASS				= TeamMeetActivity.class.getSimpleName();

	private MatesOverlay				mMatesOverlay		= null;
	private SelfOverlay					mSelfOverlay		= null;
	private MapView						mMapView			= null;
	private LocationFollower			mLocationFollower	= null;
	private TeamMeetServiceConnection	mServiceConnection	= null;
	private MapController				mMapController		= null;
	private ToastDisposerSingleton		mToastSingleton		= null;
	private List<Overlay>				mListOfOverlays		= null;

	private boolean						mFollowingLocation	= false;
	private boolean						mSatelliteView		= false;
	private boolean						mFullscreen			= false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapview);

		mToastSingleton = ToastDisposerSingleton.getInstance(getApplicationContext());

		SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
		mFullscreen = settings.getBoolean(SettingsActivity.SETTING_FULLSCREEN, false);
		mFollowingLocation = settings.getBoolean(SettingsActivity.SETTING_FOLLOW_LOCATION, false);

		createOverlays();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// create the service (if it isn't already running
		final Intent intent = new Intent(getApplicationContext(), TeamMeetService.class);
		startService(intent);

		Window w = getWindow();
		w.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		if (mFullscreen) {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		mMapView = (MapView) findViewById(R.id.mapview);
		Log.e(CLASS, "mMapView:" + mMapView.toString());
		mMapView.setBuiltInZoomControls(false);
		mMapView.setSatellite(mSatelliteView);
		mMapController = mMapView.getController();
		mMapController.setZoom(mMapView.getMaxZoomLevel());
		mListOfOverlays = mMapView.getOverlays();

		if (mFollowingLocation) {
			mLocationFollower = new LocationFollower(mMapController);
		}

		// now connect to the service
		mServiceConnection = new TeamMeetServiceConnection();
		final boolean bindSuccess = bindService(intent, mServiceConnection, 0);
		if (bindSuccess) {
			Log.e(CLASS, "bind succeeded");
			addOverlays();
			// TODO investigate: something goes wrong here the first
			// time, but is called a second time
			mServiceConnection.registerMatesUpdates(mMatesOverlay);
			mServiceConnection.registerLocationUpdates(mSelfOverlay);
			if (mFollowingLocation && mLocationFollower != null) {
				mServiceConnection.registerLocationUpdates(mLocationFollower);
			}
		} else {
			Log.e(CLASS, "bind failed");
			showError("Couldn't connect to service.");
			this.finish();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mServiceConnection != null) {
			unbindService(mServiceConnection);
		}
		mListOfOverlays.clear();
	}

	@Override
	protected void onDestroy() {
		final Intent intent = new Intent(this, TeamMeetService.class);
		stopService(intent);
		super.onDestroy();
	}

	private void createOverlays() {
		mSelfOverlay = new SelfOverlay(getResources());
		mMatesOverlay = new MatesOverlay(getResources());

	}

	private void addOverlays() {
		mListOfOverlays.add(mSelfOverlay);
		mListOfOverlays.add(mMatesOverlay);
	}

	private void showToast(final String message) {
		mToastSingleton.addLongToast(message);
	}

	private void showError(final String message) {
		showToast("Error:\n" + message);
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {

		switch (item.getItemId()) {
			case R.id.goto_mylocation:
				focusCurrentLocation();
				break;

			case R.id.auto_center:
				toggleFollowingLocation();
				break;

			case R.id.satellite_view:
				toggleSatelliteView();
				break;

			case R.id.fullscreen:
				toggleFullscreen();
				break;

			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void toggleFollowingLocation() {
		if (mFollowingLocation) {
			mServiceConnection.unregisterLocationUpdates(mLocationFollower);
			mFollowingLocation = false;
		} else {
			if (mLocationFollower == null) {
				mLocationFollower = new LocationFollower(mMapController);
			}
			mServiceConnection.registerLocationUpdates(mLocationFollower);
			mFollowingLocation = true;
		}
	}

	private void toggleSatelliteView() {
		mSatelliteView = !mSatelliteView;
		mMapView.setSatellite(mSatelliteView);
	}

	private void focusCurrentLocation() {
		if (mSelfOverlay.mCurrentLocation != null) {
			mMapController.setCenter(mSelfOverlay.mCurrentLocation);
		}
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