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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import de.teammeet.interfaces.IMatesUpdateRecipient;

public class MatesOverlay extends Overlay implements IMatesUpdateRecipient {

	private static final String	CLASS		= MatesOverlay.class.getSimpleName();

	private Map<String, Mate>			mMates		= null;
	private Resources			mResources	= null;
	private final ReentrantLock	mLock		= new ReentrantLock();

	public MatesOverlay(final Resources res) {
		mMates = new HashMap<String, Mate>();
		mResources = res;
	}

	@Override
	public boolean draw(final Canvas canvas, final MapView mapView, final boolean shadow, final long when) {
		super.draw(canvas, mapView, shadow);

		if (mMates != null) {
			final Paint paintPlayer = new Paint();
			paintPlayer.setStyle(Paint.Style.FILL);
			paintPlayer.setColor(mResources.getColor(R.color.paint_mates));

			// translate the GeoPoint to screen pixels
			acquireLock();
			try {
				final Point coords = new Point();
				for (final Mate mate : mMates.values()) {
					final GeoPoint point = mate.getLocation();
					mapView.getProjection().toPixels(point, coords);
					canvas.drawCircle(coords.x, coords.y, 5, paintPlayer);
				}
			} finally {
				releaseLock();
			}
		} else {
			// Log.e(CLASS, "WARNING mMates is null!");
		}
		return true;
	}

	@Override
	public void handleMateUpdate(Mate mate) {
		Log.d(CLASS, "MatesOverlay.handleMateUpdate() : " + mate.getID());
		acquireLock();
		try {
			mMates.put(mate.getID(), mate);
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

}
