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

import java.util.concurrent.locks.ReentrantLock;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import de.teammeet.interfaces.ILocationUpdateRecipient;

public class SelfOverlay extends Overlay implements ILocationUpdateRecipient {

	private double				mArrowLength		= 0;
	public GeoPoint				mCurrentLocation	= null;
	private Paint				mPaintPlayer		= null;
	private final ReentrantLock	mLock				= new ReentrantLock();
	private boolean				mHasPlayerDirection	= false;
	private float				mPlayerDirection	= 0;
	private Resources			mResources			= null;

	public SelfOverlay(final Resources res) {
		super();

		mResources = res;
		mPaintPlayer = new Paint();
		mPaintPlayer.setStyle(Paint.Style.FILL);
		mPaintPlayer.setColor(mResources.getColor(R.color.paint_default));
		mArrowLength = mResources.getInteger(R.integer.arrow_length);

	}

	@Override
	public boolean draw(final Canvas canvas, final MapView mapView, final boolean shadow, final long when) {
		super.draw(canvas, mapView, shadow);

		if (mCurrentLocation != null) {
			final Point coords = new Point();
			acquireLock();
			try { // paint player in noticeable fashion
				mapView.getProjection().toPixels(mCurrentLocation, coords);
				if (mHasPlayerDirection) {
					final Path path = new Path();
					path.moveTo(
							(float) (coords.x + mArrowLength *
									Math.sin((-mPlayerDirection + 180) * 3.14 / 180)),
							(float) (coords.y + mArrowLength *
									Math.cos((-mPlayerDirection + 180) * 3.14 / 180)));
					path.lineTo(
							(float) (coords.x + (mArrowLength / 4) *
									Math.sin((-mPlayerDirection + 90) * 3.14 / 180)),
							(float) (coords.y + (mArrowLength / 4) *
									Math.cos((-mPlayerDirection + 90) * 3.14 / 180)));
					path.lineTo(
							(float) (coords.x + (mArrowLength / 4) *
									Math.sin((-mPlayerDirection - 90) * 3.14 / 180)),
							(float) (coords.y + (mArrowLength / 4) *
									Math.cos((-mPlayerDirection - 90) * 3.14 / 180)));
					path.close();
					canvas.drawPath(path, mPaintPlayer);
				} else {
					canvas.drawCircle(coords.x, coords.y, 3, mPaintPlayer);
					mPaintPlayer.setStyle(Style.STROKE);
					canvas.drawCircle(coords.x, coords.y, 5, mPaintPlayer);
				}
			} finally {
				releaseLock();
			}
		}
		return true;
	}

	@Override
	public void handleLocationUpdate(final GeoPoint geopoint) {
		acquireLock();
		try {
			mCurrentLocation = geopoint;
		} finally {
			releaseLock();
		}
	}

	@Override
	public void onLocationAvailability() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleDirectionUpdate(final float direction) {
		acquireLock();
		try {
			mPlayerDirection = direction;
			if (!mHasPlayerDirection) {
				mHasPlayerDirection = true;
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
}
