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
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import de.teammeet.R;
import de.teammeet.activities.chat.Chat;
import de.teammeet.services.xmpp.TeamMeetPacketExtension;
import de.teammeet.services.xmpp.XMPPService;

public class MatesOverlay extends ItemizedOverlay<OverlayItem> {

	private static final String	CLASS = MatesOverlay.class.getSimpleName();

	private Context mContext = null;
	private String mOwnID = null;
	private String mTeam = null;
	private Map<String, Mate> mMates = null;
	private List<OverlayItem> mOverlayItems = null;
	private MapView mMapView = null;
	private final ReentrantLock	mLock = new ReentrantLock();
	private MateUpdateReceiver mMateUpdateReceiver = null;


	public MatesOverlay(String team, Context context, Drawable marker, MapView mapView) {
		super(boundCenterBottom(marker));
		mTeam = team;
		mContext = context;
		mMapView = mapView;
		final SharedPreferences settings =
				PreferenceManager.getDefaultSharedPreferences(mContext);
		final String userIDKey = mContext.getString(R.string.preference_user_id_key);
		final String userID = settings.getString(userIDKey, "");
//		final String serverKey = mContext.getString(R.string.preference_server_key);
//		final String server = settings.getString(serverKey, "");
//		mOwnID = String.format("%s@%s", userID, server);
		mOwnID = userID;
		mMates = new HashMap<String, Mate>();
		mOverlayItems = new ArrayList<OverlayItem>();
		populate();

		registerBroadcastReceiver();
	}

	private void registerBroadcastReceiver() {
		mMateUpdateReceiver = new MateUpdateReceiver();
		IntentFilter filter =
				new IntentFilter(mContext.getString(R.string.broadcast_action_teammate_update));
		filter.addCategory(mContext.getString(R.string.broadcast_category_location));
		mContext.registerReceiver(mMateUpdateReceiver, filter);
	}

	public void unregisterBroadcastReceiver() {
		mContext.unregisterReceiver(mMateUpdateReceiver);
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
			size = mMates.size();
		} finally {
			releaseLock();
		}
		return size;
	}

	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
		boolean isRedrawNeeded = mOverlayItems.size() > 0;
		if (isRedrawNeeded) {
			super.draw(canvas, mapView, false, when);
		}
		return isRedrawNeeded;
	}

	private class MateUpdateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String group = intent.getStringExtra(XMPPService.GROUP);
			if (mTeam.equals(group)) {
			Mate mate = parseMate(intent.getExtras());
			if (mate != null) {
				Log.d(CLASS, "MatesOverlay.MateUpdateReceiver.onReceive()");
				if (!Chat.getResource(mate.getID()).equals(mOwnID)) {
					acquireLock();
					try {
						if (mMates.containsKey(mate.getID())) {
							mMates.get(mate.getID()).setLocation(mate.getLocation(), mate.getAccuracy());
						} else {
							mMates.put(mate.getID(), mate);
							mOverlayItems.add(new MateOverlayItem(mate, mContext));
						}
					} finally {
						releaseLock();
					}
					populate();
					mMapView.postInvalidate();
				}
			} else {
				Log.e(CLASS, "Mate intent didn't contain the required information.");
			}
			} else {
				Log.d(CLASS, "Received mate update for different team.");
			}
		}

		private Mate parseMate(Bundle bundle) {
			Mate mate = null;
			String from = bundle.getString(TeamMeetPacketExtension.MATE);
			int lon = bundle.getInt(TeamMeetPacketExtension.LON, -1);
			int lat = bundle.getInt(TeamMeetPacketExtension.LAT, -1);
			int accuracy = bundle.getInt(TeamMeetPacketExtension.ACCURACY, -1);
			if (from != null && lon != -1 && lat != -1 && accuracy != -1) {
				mate = new Mate(from, new GeoPoint(lat, lon), accuracy);
			}
			return mate;
		}
	}
}
