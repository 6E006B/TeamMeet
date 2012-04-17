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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.google.android.maps.GeoPoint;

import de.teammeet.helper.ToastDisposerSingleton;
import de.teammeet.interfaces.ILocationService;
import de.teammeet.interfaces.ILocationUpdateRecipient;
import de.teammeet.xmpp.XMPPService;

public class LocationService extends Service implements ILocationService {
	private static final String							CLASS				= LocationService.class
																					.getSimpleName();
	private TeamMeetLocationListener					mLocationListener	= null;
	private Handler										mMessageHandler		= null;
	private de.teammeet.helper.ToastDisposerSingleton	mTostSingleton		= null;

	private LocationManager								mLocationManager	= null;
	private final boolean								mSensorRunning		= false;
	private SensorManager								mSensorManager		= null;

	private final ReentrantLock							mLockLocation		= new ReentrantLock();
	private final List<ILocationUpdateRecipient>		mLocationRecipients	= new ArrayList<ILocationUpdateRecipient>();

	private final IBinder								mBinder				= new LocalBinder();

	public class LocalBinder extends Binder {
		public LocationService getService() {
			Log.d(CLASS, "LocationService.LocalBinder.getService()");
			return LocationService.this;
		}
	}

	private XMPPService			mXMPPService			= null;
	private ServiceConnection	mXMPPServiceConnection	= new XMPPServiceConnection();

	private class XMPPServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(CLASS, "MainActivity.ServiceConnection.onServiceConnected('" + className + "')");
			mXMPPService = ((XMPPService.LocalBinder) binder).getService();
			mLocationListener.setXMPPService(mXMPPService);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			Log.d(CLASS, "MainActivity.ServiceConnection.onServiceDisconnected('" + className + "')");
			mXMPPService = null;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(CLASS, "LocationService.onCreate() called.");
		mMessageHandler = new Handler() {
			@Override
			public void handleMessage(final Message msg) {
				final Bundle bundle = msg.getData();
				if (bundle.containsKey("error")) {
					showError(bundle.getString("error"));
				} else if (bundle.containsKey("toast")) {
					showToast(bundle.getString("toast"));
				}
			}
		};
		mTostSingleton = ToastDisposerSingleton.getInstance(getApplicationContext());

		startLocationListener();
		activateGPS();
		activateCompass();
		bindToXMPP();

		Log.d(CLASS, "LocationService.onCreate() done");
	}

	private void bindToXMPP() {
		// create the service (if it isn't already running
		final Intent intent = new Intent(getApplicationContext(), XMPPService.class);
		startService(intent);

		// now connect to the service
		final boolean bindSuccess = bindService(intent, mXMPPServiceConnection, 0);
		if (bindSuccess) {
			Log.d(CLASS, "LocationService.bindToXMPP() succeeded");
		} else {
			Log.e(CLASS, "LocationService.bindToXMPP() failed");
			showError("Couldn't connect to XMPP service.");
		}
	}

	private void startLocationListener() {
		mLocationListener = new TeamMeetLocationListener(this, mXMPPService, mMessageHandler, getResources());
		Log.d(CLASS, "TeamMeetLocationListener started...");
	}

	private void stopLocationListener() {
		if (mLocationListener != null) {
			mLocationListener.deactivate();
		} else {
			Log.w(CLASS, "WARNING: mLocationListener was null!");
		}
	}

	private void activateGPS() {
		// Log.d(CLASS, "activateGPS() called.");
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		final Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		final String providerString = mLocationManager.getBestProvider(criteria, false);
		if (providerString != null) {
			Log.d(CLASS, "providerString is " + providerString);
			//TODO save power
			mLocationManager.requestLocationUpdates(providerString, 0, 0, mLocationListener);
			// Log.d(CLASS, "sucessfully requested location updates...");
		} else {
			Log.w(CLASS, "WARNING: providerString is null!");
			showError("You do not have any GPS device.");
		}
	}

	private void deactivateGPS() {
		Log.d(CLASS, "LocationService.deactivateGPS() called.");
		if (mLocationManager != null) {
			if (mLocationListener != null) {
				mLocationManager.removeUpdates(mLocationListener);
			} else {
				Log.w(CLASS, "WARNING: mGpsLocationListener was null!");
			}
		} else {
			Log.w(CLASS, "WARNING: mLocationManager was null!");
		}
		mLocationManager = null;
	}

	private void activateCompass() {
		Log.d(CLASS, "activateCompass()");
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
		for (Iterator<Sensor> sensors = sensorList.iterator(); sensors.hasNext();) {
			Sensor sensor = (Sensor) sensors.next();
			Log.d(CLASS, "Sensor: " + sensor.getName());
		}
		Sensor m_rotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

		if (m_rotationVectorSensor != null) {
			mSensorManager.registerListener(mLocationListener, m_rotationVectorSensor,
					SensorManager.SENSOR_DELAY_NORMAL);
		} else {
			mTostSingleton.addLongToast("No ORIENTATION Sensor");
		}
	}

	private void deactivateCompass() {
		if (mSensorRunning) {
			mSensorManager.unregisterListener(mLocationListener);
		}
	}

	@Override
	public IBinder onBind(final Intent intent) {
		Log.d(CLASS, "LocationService.onBind() called");
		return mBinder;
	}

	@Override
	public boolean onUnbind(final Intent intent) {
		Log.d(CLASS, "LocationService.onUnbind() called");
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		stopLocationListener();
		deactivateGPS();
		deactivateCompass();
		doUnbindXMPPService();
		Log.d(CLASS, "LocationService.onDestroy() called");
		super.onDestroy();
	}

	private void doUnbindXMPPService() {
		if (mXMPPServiceConnection != null) {
			unbindService(mXMPPServiceConnection);
		} else {
			Log.w(CLASS, "No XMPPServiceConnection to unbind from.");
		}
	}

	@Override
	public void registerLocationUpdates(final ILocationUpdateRecipient object) {
		// Log.d(CLASS, "registerLocationUpdates(" + object.getClass()
		// .getSimpleName() + ")");
		acquireLocationLock();
		try {
			mLocationRecipients.add(object);
		} finally {
			releaseLocationLock();
		}
	}

	@Override
	public void unregisterLocationUpdates(final ILocationUpdateRecipient object) {
		// Log.d(CLASS, "unregisterLocationUpdates(" + object.getClass()
		// .getSimpleName() + ")");
		acquireLocationLock();
		try {
			mLocationRecipients.remove(object);
		} finally {
			releaseLocationLock();
		}
	}

	public void setLocation(final GeoPoint geopoint, float accuracy) {
		acquireLocationLock();
		try {
			if (geopoint != null) {
				for (final ILocationUpdateRecipient locationRecipient : mLocationRecipients) {
					locationRecipient.handleLocationUpdate(geopoint, accuracy);
				}
			}
		} finally {
			releaseLocationLock();
		}
	}

	public void setDirection(final float direction) {
		acquireLocationLock();
		try {
			for (final ILocationUpdateRecipient locationRecipient : mLocationRecipients) {
				locationRecipient.handleDirectionUpdate(direction);
			}
		} finally {
			releaseLocationLock();
		}
	}

	private void acquireLocationLock() {
		mLockLocation.lock();
	}

	private void releaseLocationLock() {
		mLockLocation.unlock();
	}

	private void showToast(final String message) {
		mTostSingleton.addLongToast(message);
	}

	private void showError(final String message) {
		showToast("Error:\n" + message);
	}

}
