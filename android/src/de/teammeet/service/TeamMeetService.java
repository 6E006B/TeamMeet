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

import java.util.Iterator;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import de.teammeet.helper.ToastDisposerSingleton;

public class TeamMeetService extends Service {
	private static final String							CLASS					= TeamMeetService.class
																						.getSimpleName();
	private TeamMeetLocationListener					mLocationListener		= null;
	private ServiceInterfaceImpl						mServiceInterface		= null;
	private Handler										mMessageHandler			= null;
	private de.teammeet.helper.ToastDisposerSingleton	mTostSingleton			= null;

	private LocationManager								mLocationManager		= null;
	private final boolean								mSensorRunning			= false;
	private SensorManager								mSensorManager			= null;

	@Override
	public void onCreate() {
		super.onCreate();
		// Log.e(CLASS, "TeamMeetService.onCreate() called.");
		mServiceInterface = new ServiceInterfaceImpl();
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

		// Log.e(CLASS, "TeamMeetService.onCreate() done");
	}

	private void startLocationListener() {
		mLocationListener = new TeamMeetLocationListener(mServiceInterface, mMessageHandler, getResources());
		Log.e(CLASS, "TeamMeetLocationListener started...");
	}

	private void stopLocationListener() {
		if (mLocationListener != null) {
			mLocationListener.deactivate();
		} else {
			Log.e(CLASS, "WARNING: mLocationListener was null!");
		}
	}

	private void activateGPS() {
		// Log.e(CLASS, "activateGPS() called.");
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		final Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		final String providerString = mLocationManager.getBestProvider(criteria, false);
		if (providerString != null) {
			Log.e(CLASS, "providerString is " + providerString);
			mLocationManager.requestLocationUpdates(providerString, 0, 0, mLocationListener); // TODO
			// save
			// power
			// Log.e(CLASS, "sucessfully requested location updates...");
		} else {
			Log.e(CLASS, "WARNING: providerString is null!");
			showError("You do not have any GPS device.");
		}
	}

	private void deactivateGPS() {
		Log.e(CLASS, "TeamMeetService.deactivateGPS() called.");
		if (mLocationManager != null) {
			if (mLocationListener != null) {
				mLocationManager.removeUpdates(mLocationListener);
			} else {
				Log.e(CLASS, "WARNING: mGpsLocationListener was null!");
			}
		} else {
			Log.e(CLASS, "WARNING: mLocationManager was null!");
		}
		mLocationManager = null;
	}

	private void activateCompass() {
		Log.e(CLASS, "activateCompass()");
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
		for (Iterator<Sensor> sensors = sensorList.iterator(); sensors.hasNext();) {
			Sensor sensor = (Sensor) sensors.next();
			Log.e(CLASS, "Sensor: " + sensor.getName());
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
		Log.e(CLASS, "TeamMeetService.onBind() done");
		return mServiceInterface;
	}

	@Override
	public boolean onUnbind(final Intent intent) {
		Log.e(CLASS, "TeamMeetService.onUnbind() called.");
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		stopLocationListener();
		deactivateGPS();
		deactivateCompass();

		Log.e(CLASS, "TeamMeetService.onDestroy() called");
		super.onDestroy();
	}

	private void showToast(final String message) {
		mTostSingleton.addLongToast(message);
	}

	private void showError(final String message) {
		showToast("Error:\n" + message);
	}

}
