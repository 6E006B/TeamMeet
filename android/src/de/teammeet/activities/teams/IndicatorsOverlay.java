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

import org.jivesoftware.smack.XMPPException;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import de.teammeet.R;
import de.teammeet.services.xmpp.TeamMeetPacketExtension;
import de.teammeet.services.xmpp.XMPPService;

public class IndicatorsOverlay extends ItemizedOverlay<OverlayItem> {

	private static final String	CLASS = IndicatorsOverlay.class.getSimpleName();

	private String mTeam = null;
	private Context mContext = null;
	private XMPPService mXMPPService = null;
	private Map<GeoPoint, String> mIndicators = null;
	private List<OverlayItem> mOverlayItems = null;
	private IndicatorBroadcastReceiver mIndicatorReceiver = null;
	private IndicatorRemoveBroadcastReceiver mIndicatorRemoveReceiver = null;
	private MapView mMapView = null;
	private final ReentrantLock	mLock = new ReentrantLock();



	public IndicatorsOverlay(String team, Context context, Drawable marker, MapView mapView) {
		super(boundCenterBottom(marker));
		mTeam = team;
		mContext = context;
		mMapView = mapView;
		mIndicators = new HashMap<GeoPoint, String>();
		mOverlayItems = new ArrayList<OverlayItem>();
		populate();

		registerBroadcastReceivers();
	}

	public void setXMPPService(XMPPService xmppService) {
		mXMPPService = xmppService;
	}

	private void registerBroadcastReceivers() {
		registerIndicatorBroadcastReceiver();
		registerIndicatorRemoveBroadcastReceiver();
	}

	private void registerIndicatorBroadcastReceiver() {
		mIndicatorReceiver = new IndicatorBroadcastReceiver();

		IntentFilter filter =
				new IntentFilter(mContext.getString(R.string.broadcast_action_indicator));
		filter.addCategory(mContext.getString(R.string.broadcast_category_location));
		filter.addDataScheme("location");

		mContext.registerReceiver(mIndicatorReceiver, filter);
	}

	private void registerIndicatorRemoveBroadcastReceiver() {
		mIndicatorRemoveReceiver = new IndicatorRemoveBroadcastReceiver();

		IntentFilter removeFilter =
				new IntentFilter(mContext.getString(R.string.broadcast_action_indicator_remove));
		removeFilter.addCategory(mContext.getString(R.string.broadcast_category_location));
		removeFilter.addDataScheme("location");

		mContext.registerReceiver(mIndicatorRemoveReceiver, removeFilter);
	}

	public void unregisterBroadcastReceivers() {
		mContext.unregisterReceiver(mIndicatorReceiver);
		mContext.unregisterReceiver(mIndicatorRemoveReceiver);
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

	public void removeIndicator(int lon, int lat) {
		Log.d(CLASS, "IndicatorsOverlay.handleIndicatorUpdate()");
		GeoPoint location = new GeoPoint(lon, lat);
		boolean changed = false;
		acquireLock();
		try {
			if (mIndicators.containsKey(location)) {
				mIndicators.remove(location);
				OverlayItem itemToRemove = null;
				for (OverlayItem overlayItem : mOverlayItems) {
					if (location.equals(overlayItem.getPoint())) {
						itemToRemove = overlayItem;
					}
				}
				if (itemToRemove != null) {
					mOverlayItems.remove(itemToRemove);
				}
				changed = true;
			}
		} finally {
			releaseLock();
		}
		if (changed) {
			setLastFocusedIndex(-1);
			populate();
			mMapView.postInvalidate();
		}
	}

	private void acquireLock() {
		mLock.lock();
	}

	private void releaseLock() {
		mLock.unlock();
	}

	@Override
	protected OverlayItem createItem(int index) {
		OverlayItem item = null;
		acquireLock();
		try {
			item = mOverlayItems.get(index);
		} finally {
			releaseLock();
		}
		return item;
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
		
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setTitle("Indicator");
    	builder.setMessage(info+"\nWhat do you want to do?");
    	builder.setCancelable(true);
    	builder.setPositiveButton("Navigate Here", new DialogInterface.OnClickListener() {
    		@Override
    		public void onClick(DialogInterface dialog, int id) {
    			Log.d(CLASS, "User clicked Navigate Here");
    			String intentData = String.format("google.navigation:ll=%f,%f&mode=w",
    			                                  location.getLatitudeE6() * 1E-6,
    			                                  location.getLongitudeE6() * 1E-6);
    			Intent navigationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(intentData));
    			mContext.startActivity(navigationIntent);
			}
    	});
    	builder.setNegativeButton("Remove Indicator", new DialogInterface.OnClickListener() {
    		@Override
    		public void onClick(DialogInterface dialog, int id) {
    			Log.d(CLASS, "User clicked Remove Indicator");
    			if (mXMPPService != null) {
    				try {
						mXMPPService.sendIndicator(location, info, true);
					} catch (XMPPException e) {
						Log.e(CLASS, "Unable to remove indicator:" + e.getLocalizedMessage());
						e.printStackTrace();
					}
    			} else {
    				Log.w(CLASS, "XMPPService unavailable");
    			}
    		}
    	});
    	builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
    	final AlertDialog alert = builder.create();
    	mMapView.post(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				alert.show();	
			}
		});
		return super.onTap(index);
	}

	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
		int size = 0;
		acquireLock();
		try {
			size = mOverlayItems.size();
		} finally {
			releaseLock();
		}
		boolean isRedrawNeeded = size > 0;
		if (isRedrawNeeded) {
			super.draw(canvas, mapView, shadow, when);
		}
		return isRedrawNeeded;
	}

	private class IndicatorBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(CLASS, "*** Received INDICATOR broadcast");
			if (mTeam.equals(intent.getStringExtra(XMPPService.GROUP))) {
				int lon = intent.getIntExtra(TeamMeetPacketExtension.LON, -1);
				int lat = intent.getIntExtra(TeamMeetPacketExtension.LAT, -1);
				String info = intent.getStringExtra(TeamMeetPacketExtension.INFO);
				if (lon != -1 && lat != -1 && info != null) {
					handleIndicatorUpdate(lon, lat, info);
				}
			} else {
				Log.d(CLASS, "indicator for different team received.");
			}
		}
	}

	private class IndicatorRemoveBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (mTeam.equals(intent.getStringExtra(XMPPService.GROUP))) {
				Log.d(CLASS, "*** Received INDICATOR_REMOVE broadcast");
				int lon = intent.getIntExtra(TeamMeetPacketExtension.LON, -1);
				int lat = intent.getIntExtra(TeamMeetPacketExtension.LAT, -1);
				if (lon != -1 && lat != -1) {
					removeIndicator(lon, lat);
				}
			} else {
				Log.d(CLASS, "remove indicator for different team received.");
			}
		}
	}
}
