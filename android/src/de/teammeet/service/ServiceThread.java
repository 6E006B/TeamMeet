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

import android.content.res.Resources;
import android.os.Handler;
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
	private GeoPoint				mLocation				= null;
	private ServerCommunication		mServerCommunication	= null;
	private ServiceState			mState					= ServiceState.ACTIVE;

	public ServiceThread(final ServiceInterfaceImpl serviceInterface, final Handler messageHandler,
			final Resources res) {
		this.mServiceInterface = serviceInterface;
		mResources = res;
		this.mServerCommunication = new ServerCommunication(messageHandler, mResources);
		mTimeout = mResources.getInteger(R.integer.server_timeout);
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
						mServiceInterface.setMates(mServerCommunication
								.sendPositionAndGetMatesFromServer(mLocation));
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
		// Log.e(CLASS, "ServiceThread.run() finished.");
	}

	public void deactivateThread() {
		if (mState == ServiceState.ACTIVE) {
			mState = ServiceState.LOGOUT_AND_DIE;
		} else {
			mState = ServiceState.DIE;
		}
	}

	public void setLocation(final GeoPoint geopoint) {
		// Log.e(CLASS, "new mLocation set: " + geopoint.toString());
		mLocation = geopoint;
		mServiceInterface.setLocation(geopoint);
	}

	public void signalGPSDisabled() {
		// Log.e(CLASS, "ServiceThread.signalGPSDisabled() called.");
	}

	public void signalGPSEnabled() {
		// Log.e(CLASS, "ServiceThread.signalGPSEnabled() called.");
	}

	public void signalGPSStautsChange(final int status) {
		// Log.e(CLASS,
		// "ServiceThread.signalGPSStatusChange(" + status + ") called.");
	}

	public void updateDirection(final float direction) {
		// Log.e(CLASS, "new mLocation set: " + direction);
		mServiceInterface.setDirection(direction);
	}
}
