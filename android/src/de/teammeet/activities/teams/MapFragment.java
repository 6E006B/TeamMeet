package de.teammeet.activities.teams;

import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import de.teammeet.R;
import de.teammeet.services.xmpp.XMPPService;

/**
 * This is the Fragment class that will hold the MapView as its content
 * view.
 *
 */
public class MapFragment extends SherlockFragment {
	private static final String CLASS = MapFragment.class.getSimpleName();

	private MapView mMapView;
	private List<Overlay> mListOfOverlays = null;

	private MyDirectionLocationOverlay mMyLocationOverlay = null;
	private MatesOverlay mMatesOverlay = null;
	private IndicatorsOverlay mIndicatorsOverlay = null;
	private MapGestureDetectorOverlay mMapGestureOverlay = null;

	private String mTeam = null;
	private boolean	mFollowingLocation = false;
	private boolean	mSatelliteView = false;
	private boolean	mFullscreen	= false;

	private XMPPService mXMPPService = null;
	private XMPPServiceConnection mXMPPServiceConnection = new XMPPServiceConnection();
	private Bundle mCurrentArgs = null;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);

		Log.d(CLASS, "MapFragment.onCreate()");

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		final String fullscreenKey = getString(R.string.preference_fullscreen_key);
		mFullscreen = settings.getBoolean(fullscreenKey, false);
		final String followLocationKey = getString(R.string.preference_auto_center_key);
		mFollowingLocation = settings.getBoolean(followLocationKey, false);
		final String satelliteViewKey = getString(R.string.preference_satellite_key);
		mSatelliteView = settings.getBoolean(satelliteViewKey, false);

		mCurrentArgs = getArguments();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup vg, Bundle data) {

		// Initialize new MapView, if there is none yet
		if (mMapView == null) {
			mMapView = (MapView)inflater.inflate(R.layout.mapview, vg, false);
			Log.d(CLASS, String.format("MapFragment.onCreateView() => %s", mMapView));

			mMapView.setBuiltInZoomControls(true);
			mMapView.setSatellite(mSatelliteView);
		} else {
			((ViewGroup)mMapView.getParent()).removeAllViews();
		}

		return mMapView;
	}

	@Override
	public void onResume() {
		super.onResume();

		Log.d(CLASS, "MapFragment.onResume()");

		Activity parent = getActivity();

		// create the service (if it isn't already running)
		final Intent xmppIntent = new Intent(parent, XMPPService.class);
		parent.startService(xmppIntent);

		Log.d(CLASS, "started location service");

		Window window = parent.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		if (mFullscreen) {
			window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		mListOfOverlays = mMapView.getOverlays();

		// now connect to the service
		final boolean bindSuccess = parent.bindService(xmppIntent, mXMPPServiceConnection, 0);
		if (bindSuccess) {
			Log.d(CLASS, "TeamMeetActivity.onResume() bind to XMPP service succeeded");
		} else {
			Log.e(CLASS, "TeamMeetActivity.onResume() bind to XMPP service failed");
			Toast.makeText(getActivity(), "Error: Couldn't connect to XMPP service.",
			               Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onPause() {
		if (mIndicatorsOverlay != null) {
			mIndicatorsOverlay.unregisterBroadcastReceivers();
		}
		if (mMatesOverlay != null) {
			mMatesOverlay.unregisterBroadcastReceiver();
		}
		if (mMyLocationOverlay != null) {
			mMyLocationOverlay.disableCompass();
		}
		//unregisterMatesUpdates();
		if (mXMPPServiceConnection != null) {
			getActivity().unbindService(mXMPPServiceConnection);
		}
		mXMPPService = null;
		mListOfOverlays.clear();
		super.onPause();
	}


	private void createOverlays(String team) {
		Context context = getActivity();
		mMatesOverlay = new MatesOverlay(team, context,
		                                 getResources().getDrawable(R.drawable.ic_map_mate),
		                                 mMapView);
		mMapGestureOverlay = new MapGestureDetectorOverlay(mMapView, context);
		mIndicatorsOverlay = new IndicatorsOverlay(team, context,
		                                           getResources().getDrawable(R.drawable.ic_map_indicator),
		                                           mMapView);
		mMyLocationOverlay = new MyDirectionLocationOverlay(context, mMapView);

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

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.map, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

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

	private void focusCurrentLocation() {
		mMyLocationOverlay.focusCurrentLocation();
	}

	private void toggleFollowingLocation() {
		mFollowingLocation = !mFollowingLocation;
		mMyLocationOverlay.followLocation(mFollowingLocation);
	}

	private void toggleSatelliteView() {
		mSatelliteView = !mSatelliteView;
		mMapView.setSatellite(mSatelliteView);
	}

	private void toggleFullscreen() {
		mFullscreen = !mFullscreen;
		Window window = getActivity().getWindow();
		if (mFullscreen) {
			window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
	}

	private boolean handleFragmentArgs(Bundle args) {
		boolean success = true;
		mTeam = args.getString(XMPPService.GROUP);
		if (mTeam != null) {
			createOverlays(mTeam);
			addOverlays();
		} else {
			final Activity parent;
			success = false;
			final String error = "Intent had no team!";
			Log.e(CLASS, error);
			parent = getActivity();
			parent.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(parent, error, Toast.LENGTH_LONG).show();
					parent.finish();
				}
			});
		}
		return success;
	}

	private class XMPPServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(CLASS, "TeamMeetActivity.XMPPServiceConnection.onServiceConnected('" + className + "')");
			mXMPPService = ((XMPPService.LocalBinder) binder).getService();

			if (handleFragmentArgs(mCurrentArgs)) {
				mMapGestureOverlay.setXMPPService(mXMPPService);
				mIndicatorsOverlay.setXMPPService(mXMPPService);
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
}
