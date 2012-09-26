package de.teammeet.activities.teams;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.android.maps.MapView;

import de.teammeet.R;

/**
 * This is the Fragment class that will hold the MapView as its content
 * view.
 *
 */
public class MapFragment extends SherlockFragment {
	private static final String CLASS = MapFragment.class.getSimpleName();

	private MapView mMapView;

	@Override
	public void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setRetainInstance(true);
		Log.d(CLASS, "MapFragment.onCreate()");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup vg, Bundle data) {

		// Initialize new MapView, if there is none yet
		if (mMapView == null) {
			mMapView = (MapView)inflater.inflate(R.layout.mapview, vg, false);
			Log.d(CLASS, String.format("MapFragment.onCreateView() => %s", mMapView));

			mMapView.setBuiltInZoomControls(true);
		} else {
			((ViewGroup)mMapView.getParent()).removeAllViews();
		}

		return mMapView;
	}
}
