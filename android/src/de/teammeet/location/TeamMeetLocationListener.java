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

package de.teammeet.location;

import java.util.Timer;
import java.util.TimerTask;

import org.jivesoftware.smack.XMPPException;

import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.maps.GeoPoint;

import de.teammeet.R;
import de.teammeet.xmpp.XMPPService;

public class TeamMeetLocationListener implements LocationListener, SensorEventListener {

	private static final String	CLASS				= TeamMeetLocationListener.class.getSimpleName();

	private Resources			mResources			= null;
	private int					mTimeout			= 0;

	private LocationService		mLocationService	= null;
	private XMPPService			mXMPPService		= null;
	private Handler				mMessageHandler		= null;
	protected GeoPoint			mLocation			= null;
	protected GeoPoint			mLastLocation		= null;
	protected float				mAccuracy			= 0;

	private Timer				mTimer				= null;
	private TimerTask			mTimerTask			= null;

	public TeamMeetLocationListener(final LocationService serviceInterface, final XMPPService xmppService,
			final Handler messageHandler, final Resources res) {
		mLocationService = serviceInterface;
		mXMPPService = xmppService;
		mMessageHandler = messageHandler;
		mResources = res;
		mTimeout = mResources.getInteger(R.integer.server_timeout);
		mTimer = new Timer(CLASS, true);
		mTimerTask = new TimerTask() {

			@Override
			public void run() {
				if (mLocation != null && mLocation != mLastLocation) {
					try {
						mXMPPService.sendLocation(mLocation, mAccuracy);
					} catch (XMPPException e) {
						e.printStackTrace();
						Log.e(CLASS, "Error while sending location: " + e.toString());
					}
					showToast("Location update to: " + mLocation.toString());
					Log.d(CLASS, "Location update to: " + mLocation.toString());
					mLastLocation = mLocation;
				}
			}
		};
		mTimer.scheduleAtFixedRate(mTimerTask, mTimeout, mTimeout);
	}

	private void showToast(final String message) {
		if (mMessageHandler != null) {
			final Message msg = new Message();
			final Bundle bundle = new Bundle();
			bundle.putString("toast", message);
			msg.setData(bundle);
			mMessageHandler.sendMessage(msg);
		}
	}

	private void showError(final String message) {
		if (mMessageHandler != null) {
			final Message msg = new Message();
			final Bundle bundle = new Bundle();
			bundle.putString("error", message);
			msg.setData(bundle);
			mMessageHandler.sendMessage(msg);
		}
	}

	public void deactivate() {
		mTimerTask.cancel();
	}

	@Override
	public void onLocationChanged(final Location location) {

		// Log.e(CLASS, "GpsLocationListener.onLocationChanged(" + location
		// .toString() + ")");
		final GeoPoint geopoint = new GeoPoint((int) (location.getLatitude() * 1E6),
				(int) (location.getLongitude() * 1E6));
		mLocation = geopoint;
		mAccuracy = location.getAccuracy();
		mLocationService.setLocation(mLocation, mAccuracy);
	}

	@Override
	public void onProviderDisabled(final String provider) {
		// TODO handle if provider gets disabled and probably also if provided
		// isn't enabled in the first place
		Log.e(CLASS, "TeamMeetLocationListener.onProviderDisabled() called.");
		showError("Please enable your GPS.");
	}

	@Override
	public void onProviderEnabled(final String provider) {
		// TODO handle activation of provider?
		Log.e(CLASS, "TeamMeetLocationListener.onProviderEnabled() called.");
	}

	@Override
	public void onStatusChanged(final String provider, final int status, final Bundle extras) {
		// TODO handle status changes
		Log.e(CLASS, "TeamMeetLocationListener.onStatusChange(" + status + ") called.");
	}

	@Override
	public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
		// NOTE: this is the accuracy of the compass not the gps
	}

	@Override
	public void onSensorChanged(final SensorEvent event) {
		// Log.e(CLASS, "new mLocation set: " + direction);
		mLocationService.setDirection(event.values[0]);
	}
}
