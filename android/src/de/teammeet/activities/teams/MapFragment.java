package de.teammeet.activities.teams;

import android.os.Bundle;
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

	//public static final String TAG = "mapFragment";

	public MapFragment() {}

	@Override
	public void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup vg, Bundle data) {

		// Initialize MapView
		MapView mapView = (MapView)inflater.inflate(R.layout.mapview, vg, false);

		mapView.setClickable(true);
		mapView.setBuiltInZoomControls(true); // If you want.

		return mapView;
	}
}
