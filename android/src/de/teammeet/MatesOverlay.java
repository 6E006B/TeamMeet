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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

import de.teammeet.interfaces.IMatesUpdateRecipient;

public class MatesOverlay extends ItemizedOverlay<OverlayItem> implements IMatesUpdateRecipient {

	private static final String	CLASS = MatesOverlay.class.getSimpleName();

	private Map<String, Mate> mMates = null;
	private List<OverlayItem> mOverlayItems = null;
	private final ReentrantLock	mLock = new ReentrantLock();


	public MatesOverlay(Drawable marker) {
		super(marker);
		mMates = new HashMap<String, Mate>();
		mOverlayItems = new ArrayList<OverlayItem>();
	}

	@Override
	public void handleMateUpdate(Mate mate) {
		Log.d(CLASS, "MatesOverlay.handleMateUpdate() : " + mate.getID());
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
		return mMates.size();
	}

}
