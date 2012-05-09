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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import de.teammeet.R;
import de.teammeet.interfaces.IMatesUpdateRecipient;

public class MatesOverlay extends ItemizedOverlay<OverlayItem> implements IMatesUpdateRecipient {

	private static final String	CLASS = MatesOverlay.class.getSimpleName();

	private Context mContext = null;
	private String mOwnID = null;
	private Map<String, Mate> mMates = null;
	private List<OverlayItem> mOverlayItems = null;
	private MapView mMapView = null;
	private final ReentrantLock	mLock = new ReentrantLock();


	public MatesOverlay(Context context, Drawable marker, MapView mapView) {
		super(boundCenterBottom(marker));
		mContext = context;
		mMapView = mapView;
		final SharedPreferences settings =
				PreferenceManager.getDefaultSharedPreferences(mContext);
		final String userIDKey = mContext.getString(R.string.preference_user_id_key);
		final String userID = settings.getString(userIDKey, "");
		final String serverKey = mContext.getString(R.string.preference_server_key);
		final String server = settings.getString(serverKey, "");
		mOwnID = String.format("%s@%s", userID, server);
		mMates = new HashMap<String, Mate>();
		mOverlayItems = new ArrayList<OverlayItem>();
		populate();
	}

	@Override
	public void handleMateUpdate(Mate mate) {
		Log.d(CLASS, "MatesOverlay.handleMateUpdate() : " + mate.getID());
		if (!mate.getID().equals(mOwnID)) {
			acquireLock();
			try {
				if (mMates.containsKey(mate.getID())) {
					mMates.get(mate.getID()).setLocation(mate.getLocation(), mate.getAccuracy());
				} else {
					mMates.put(mate.getID(), mate);
					mOverlayItems.add(new MateOverlayItem(mate));
				}
			} finally {
				releaseLock();
			}
			populate();
			mMapView.invalidate();
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
	protected boolean onTap(int index) {
		final String mateID = ((MateOverlayItem)mOverlayItems.get(index)).getMate().getID();
		final String mateNick = mateID.substring(mateID.lastIndexOf("/")+1);
		Toast.makeText(mContext, mateNick, Toast.LENGTH_SHORT).show();
		return super.onTap(index);
	}
}
