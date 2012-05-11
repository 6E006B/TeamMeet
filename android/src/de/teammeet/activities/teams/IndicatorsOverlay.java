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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import de.teammeet.R;
import de.teammeet.services.xmpp.TeamMeetPacketExtension;

public class IndicatorsOverlay extends ItemizedOverlay<OverlayItem> {

	private static final String	CLASS = IndicatorsOverlay.class.getSimpleName();

	private Context mContext = null;
	private Map<GeoPoint, String> mIndicators = null;
	private List<OverlayItem> mOverlayItems = null;
	private IndicatorBroadcastReceiver mIndicatorReceiver = null;
	private MapView mMapView = null;
	private final ReentrantLock	mLock = new ReentrantLock();

	public IndicatorsOverlay(Context context, Drawable marker, MapView mapView) {
		super(boundCenterBottom(marker));
		mContext = context;
		mMapView = mapView;
		mIndicators = new HashMap<GeoPoint, String>();
		mOverlayItems = new ArrayList<OverlayItem>();
		populate();
		mIndicatorReceiver = new IndicatorBroadcastReceiver();
		IntentFilter filter =
				new IntentFilter(mContext.getString(R.string.broadcast_action_indicator));
		filter.addCategory(mContext.getString(R.string.broadcast_category_location));
		filter.addDataScheme("location");
		mContext.registerReceiver(mIndicatorReceiver, filter);
	}

	public void handleIndicatorUpdate(int lon, int lat, String info) {
		Log.d(CLASS, "IndicatorsOverlay.handleIndicatorUpdate()");
		GeoPoint location = new GeoPoint(lon, lat);
		acquireLock();
		try {
			if (mIndicators.containsKey(location)) {
				mIndicators.put(location, info);
			} else {
				mIndicators.put(location, info);
				mOverlayItems.add(new OverlayItem(location, info, info));
			}
		} finally {
			releaseLock();
		}
		populate();
		mMapView.postInvalidate();
	}

	private void acquireLock() {
		mLock.lock();
	}

	private void releaseLock() {
		mLock.unlock();
	}

	@Override
	protected OverlayItem createItem(int index) {
//		Log.d(CLASS, "MatesOverlay.createItem("+index+") on position: " +
//				mOverlayItems.get(index).getPoint().toString());
		return mOverlayItems.get(index);
	}

	@Override
	public int size() {
		int size = 0;
		acquireLock();
		try {
			size = mIndicators.size();
		} finally {
			releaseLock();
		}
		return size;
	}

	@Override
	protected boolean onTap(int index) {
		final GeoPoint location = mOverlayItems.get(index).getPoint();
		final String info = mIndicators.get(location);
		Toast.makeText(mContext, info, Toast.LENGTH_SHORT).show();
		return super.onTap(index);
	}

	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
		boolean isRedrawNeeded = mOverlayItems.size() > 0;
		if (isRedrawNeeded) {
			super.draw(canvas, mapView, shadow, when);
		}
		return isRedrawNeeded;
	}

	private class IndicatorBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(CLASS, "*** Received INDICATOR broadcast");
			int lon = intent.getIntExtra(TeamMeetPacketExtension.LON, -1);
			int lat = intent.getIntExtra(TeamMeetPacketExtension.LAT, -1);
			String info = intent.getStringExtra(TeamMeetPacketExtension.INFO);
			if (lon != -1 && lat != -1 && info != null) {
				handleIndicatorUpdate(lon, lat, info);
			}
		}
	}
}
