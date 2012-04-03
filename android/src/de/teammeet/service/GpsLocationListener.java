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

package de.teammeet.service;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import com.google.android.maps.GeoPoint;

public class GpsLocationListener implements LocationListener {

	// private static final String CLASS = GpsLocationListener.class
	// .getSimpleName();
	private ServiceThread	mServiceThread	= null;

	public GpsLocationListener(final ServiceThread serviceThread) {
		this.mServiceThread = serviceThread;
	}

	@Override
	public void onLocationChanged(final Location location) {
		// Log.e(CLASS, "GpsLocationListener.onLocationChanged(" + location
		// .toString() + ")");
		final GeoPoint geopoint = new GeoPoint((int) (location.getLatitude() * 1E6),
				(int) (location.getLongitude() * 1E6));
		mServiceThread.setLocation(geopoint);
	}

	@Override
	public void onProviderDisabled(final String provider) {
		// TODO handle if provider gets disabled and probably also if provided
		// isn't enabled in the first place
		mServiceThread.signalGPSDisabled();
	}

	@Override
	public void onProviderEnabled(final String provider) {
		// TODO handle activation of provider?
		mServiceThread.signalGPSEnabled();
	}

	@Override
	public void onStatusChanged(final String provider, final int status, final Bundle extras) {
		// TODO handle status changes
		mServiceThread.signalGPSStautsChange(status);
	}

}
