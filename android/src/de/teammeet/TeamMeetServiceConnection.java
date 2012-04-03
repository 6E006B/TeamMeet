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

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import de.teammeet.interfaces.ILocationUpdateRecipient;
import de.teammeet.interfaces.IMatesUpdateRecipient;
import de.teammeet.interfaces.IService;

public class TeamMeetServiceConnection implements ServiceConnection {

	private static final String							CLASS				= TeamMeetServiceConnection.class
																					.getSimpleName();
	private final ArrayList<ILocationUpdateRecipient>	mLocationRecipients	= new ArrayList<ILocationUpdateRecipient>();
	private final ArrayList<IMatesUpdateRecipient>		mMatesRecipients	= new ArrayList<IMatesUpdateRecipient>();
	private IService									mService			= null;
	private boolean										mConnected			= false;

	@Override
	public void onServiceDisconnected(final ComponentName name) {
		for (final ILocationUpdateRecipient object : mLocationRecipients) {
			mService.unregisterLocationUpdates(object);
		}
		for (final IMatesUpdateRecipient object : mMatesRecipients) {
			mService.unregisterMatesUpdates(object);
		}
		mConnected = false;
		// Log.e(CLASS, "onServiceDisconnected() done");
	}

	@Override
	public void onServiceConnected(final ComponentName name, final IBinder binder) {
		mService = (IService) binder;
		for (final ILocationUpdateRecipient object : mLocationRecipients) {
			mService.registerLocationUpdates(object);
		}
		for (final IMatesUpdateRecipient object : mMatesRecipients) {
			mService.registerMatesUpdates(object);
		}
		mConnected = true;
		Log.e(CLASS, "onServiceConnected() done");
	}

	public void registerLocationUpdates(final ILocationUpdateRecipient object) {
		assert (object != null);
		if (!mLocationRecipients.contains(object)) {
			if (mConnected) {
				mService.registerLocationUpdates(object);
			} else {
				Log.e(CLASS, "WARNING: trying to register location updates while mService==null!");
			}
			mLocationRecipients.add(object);
		} else {
			Log.e(CLASS, "WARNING: object tried to reregister for location updates: " + object.toString());
		}
	}

	public void registerMatesUpdates(final IMatesUpdateRecipient object) {
		assert (object != null);
		if (!mMatesRecipients.contains(object)) {
			if (mConnected) {
				mService.registerMatesUpdates(object);
			} else {
				Log.e(CLASS, "WARNING: trying to register mates updates while mService==null!");
			}
			mMatesRecipients.add(object);
		} else {
			Log.e(CLASS, "WARNING: object tried to reregister for mates updates: " + object.toString());
		}
	}

	public void unregisterLocationUpdates(final ILocationUpdateRecipient object) {
		assert (object != null);
		if (mLocationRecipients.contains(object)) {
			mLocationRecipients.remove(object);
			if (mConnected) {
				mService.unregisterLocationUpdates(object);
			} else {
				Log.e(CLASS, "WARNING: trying to unregister location updates while mService==null!");
			}
		} else {
			Log.e(CLASS,
					"WARNING: not previously for location updates registered object tried to unregister: " +
							object.toString());
		}
	}

	public void unregisterMatesUpdates(final IMatesUpdateRecipient object) {
		assert (object != null);
		if (mMatesRecipients.contains(object)) {
			mMatesRecipients.remove(object);
			if (mConnected) {
				mService.unregisterMatesUpdates(object);
			} else {
				Log.e(CLASS, "WARNING: trying to unregister mates updates while mService==null!");
			}
		} else {
			Log.e(CLASS, "WARNING: not previously for mates updates registered object tried to unregister: " +
					object.toString());
		}
	}

}
