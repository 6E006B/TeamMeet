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

import java.util.Timer;
import java.util.TimerTask;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.maps.GeoPoint;

import de.teammeet.R;
import de.teammeet.helper.ServerCommunication;

public class ServiceThread extends Thread {

	public enum ServiceState {
		INACTIVE, ACTIVE, AWAITING_LOCATION, LOGIN, LOGOUT, DIE, LOGOUT_AND_DIE
	}

	private static final String		CLASS					= ServiceThread.class.getSimpleName();

	private Resources				mResources				= null;
	private int						mTimeout				= 0;

	private ServiceInterfaceImpl	mServiceInterface		= null;
	private Handler					mMessageHandler			= null;
	protected GeoPoint				mLocation				= null;
	protected GeoPoint				mLastLocation			= null;
	protected float					mAccuracy				= 0;
	private ServerCommunication		mServerCommunication	= null;
	private ServiceState			mState					= ServiceState.ACTIVE;

	public ServiceThread(final ServiceInterfaceImpl serviceInterface, final Handler messageHandler,
			final Resources res) {
		mServiceInterface = serviceInterface;
		mMessageHandler = messageHandler;
		mResources = res;
		mServerCommunication = new ServerCommunication(mMessageHandler, mResources);
		mTimeout = mResources.getInteger(R.integer.server_timeout);
		Timer timer = new Timer(getName(), true);
		TimerTask timerTask = new TimerTask() {

			@Override
			public void run() {
				if (mLocation != null && mLocation != mLastLocation) {
					serviceInterface.sendLocation(mLocation, mAccuracy);
					showToast("Location update to: " + mLocation.toString());
					Log.d(CLASS, "Location update to: " + mLocation.toString());
					mLastLocation = mLocation;
				}
			}
		};
		timer.scheduleAtFixedRate(timerTask, mTimeout, mTimeout);
	}

	@Override
	public void run() {
		// Log.e(CLASS, "ServiceThread.run() called.");
		while (mState != ServiceState.DIE) {
			if (mLocation != null) {
				switch (mState) {
					case ACTIVE:
						// send position to server and fetch team mates from
						// server and pass to Service Interface
						// mServiceInterface.setMates(mServerCommunication
						// .sendPositionAndGetMatesFromServer(mLocation));
						break;
					case LOGIN:
						mServerCommunication.registerAtServer(mLocation);
						// mServiceInterface.signalRegisteredAtServer(); TODO
						mState = ServiceState.ACTIVE;
						break;
					case LOGOUT:
						mServerCommunication.logout();
						// mServiceInterface.signalLogout(); TODO
						mState = ServiceState.ACTIVE;
						break;
					case LOGOUT_AND_DIE:
						mServerCommunication.logout();
						mState = ServiceState.DIE;
						break;
					// TODO are "ACTIVE" and "INACTIVE" both needed?
					case INACTIVE:
					default:
						break;
				}
			}
			try {
				Thread.sleep(mTimeout);
			} catch (final InterruptedException e) {
				e.printStackTrace();
				Log.e(CLASS, "sleep failed:" + e.getMessage());
			}
		}
		Log.e(CLASS, "ServiceThread.run() finished.");
	}

	public void deactivateThread() {
		if (mState == ServiceState.ACTIVE) {
			mState = ServiceState.LOGOUT_AND_DIE;
		} else {
			mState = ServiceState.DIE;
		}
	}

	public void setLocation(final GeoPoint geopoint, float accuracy) {
		// Log.e(CLASS, "new mLocation set: " + geopoint.toString());
		mLocation = geopoint;
		mAccuracy = accuracy;
		mServiceInterface.setLocation(geopoint, accuracy);
	}

	public void signalGPSDisabled() {
		Log.e(CLASS, "ServiceThread.signalGPSDisabled() called.");
		showError("Please enable your GPS.");
	}

	public void signalGPSEnabled() {
		Log.e(CLASS, "ServiceThread.signalGPSEnabled() called.");
	}

	public void signalGPSStautsChange(final int status) {
		Log.e(CLASS, "ServiceThread.signalGPSStatusChange(" + status + ") called.");
	}

	public void updateDirection(final float direction) {
		// Log.e(CLASS, "new mLocation set: " + direction);
		mServiceInterface.setDirection(direction);
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
}
