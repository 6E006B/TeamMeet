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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class IndicationOverlay extends Overlay {
	private static final String	CLASS					= IndicationOverlay.class.getSimpleName();
	private GeoPoint			mLatestTouchGeoPoint	= null;
	private List<String>		mAddressOfPoint			= null;

	private final ReentrantLock	mLock					= new ReentrantLock();

	private Resources			mResources				= null;
	private int					mIndicatorSize			= 0;
	private int					mIndicatorWidth			= 0;
	private Paint				mPaintIndicator			= null;
	private Paint				mPaintIndicatorText		= null;

	public IndicationOverlay(final Resources resources) {
		mResources = resources;
		mIndicatorSize = mResources.getInteger(R.integer.indicator_size);
		mIndicatorWidth = mResources.getInteger(R.integer.indicator_width);

		mPaintIndicator = new Paint();
		mPaintIndicator.setColor(mResources.getColor(R.color.paint_indicator));
		mPaintIndicator.setStrokeWidth(mIndicatorWidth);
		mPaintIndicator.setStyle(Paint.Style.STROKE);

		mPaintIndicatorText = new Paint();
		mPaintIndicatorText.setColor(mResources.getColor(R.color.paint_indicator_text));
		mPaintIndicatorText.setAntiAlias(true);
		mPaintIndicatorText.setTextSize(mResources.getInteger(R.integer.indicator_text_size));
		mPaintIndicatorText.setTypeface(Typeface.DEFAULT_BOLD);
		mPaintIndicatorText.setShadowLayer(mResources.getInteger(R.integer.indicator_text_border_width), 1,
				1, mResources.getColor(R.color.paint_indicator_text_border));
	}

	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		mLatestTouchGeoPoint = p;
		Geocoder geoCoder = new Geocoder(mapView.getContext(), Locale.getDefault());
		try {
			List<Address> addresses = geoCoder.getFromLocation(mLatestTouchGeoPoint.getLatitudeE6() / 1E6,
					mLatestTouchGeoPoint.getLongitudeE6() / 1E6, 1);

			List<String> addressStringList = new LinkedList<String>();
			if (addresses.size() > 0) {
				for (int i = 0; i < addresses.get(0).getMaxAddressLineIndex(); i++)
					addressStringList.add(addresses.get(0).getAddressLine(i));
			}
			// Collections.reverse(addressStringList);
			mAddressOfPoint = addressStringList;
		} catch (IOException e) {
			Log.e(CLASS, "Error using Geocoder: " + e.getLocalizedMessage());
			e.printStackTrace();
		}

		return super.onTap(p, mapView);
	}

	@Override
	public void draw(final Canvas canvas, final MapView mapView, final boolean shadow) {
		super.draw(canvas, mapView, shadow);
		if (mLatestTouchGeoPoint != null) {
			acquireLock();
			try {
				Projection projection = mapView.getProjection();
				final Point screenPoint = new Point();

				projection.toPixels(mLatestTouchGeoPoint, screenPoint);
				canvas.drawCircle(screenPoint.x, screenPoint.y, mIndicatorSize + 1, mPaintIndicator);

				if (mAddressOfPoint != null) {
					int x = screenPoint.x + mIndicatorSize + mIndicatorWidth;
					int y = screenPoint.y - mIndicatorSize / 2 - mIndicatorWidth / 2;
					for (String line : mAddressOfPoint) {
						canvas.drawText(line, x, y, mPaintIndicatorText);
						y -= mPaintIndicatorText.ascent() - mPaintIndicatorText.descent() * 2f;
					}
				}
			} finally {
				releaseLock();
			}
		}
	}

	private void acquireLock() {
		mLock.lock();
	}

	private void releaseLock() {
		mLock.unlock();
	}
}