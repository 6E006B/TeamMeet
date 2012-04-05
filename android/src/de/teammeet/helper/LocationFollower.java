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

package de.teammeet.helper;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;

import de.teammeet.TeamMeetServiceConnection;
import de.teammeet.interfaces.ILocationUpdateRecipient;

public class LocationFollower implements ILocationUpdateRecipient {

	private MapController				mMapController		= null;
	private TeamMeetServiceConnection	mServiceConnection	= null;
	private boolean						mActive				= false;

	public LocationFollower(final MapController mapController, TeamMeetServiceConnection serviceConnection) {
		mMapController = mapController;
		mServiceConnection = serviceConnection;
	}

	@Override
	public void handleLocationUpdate(final GeoPoint geopoint) {
		if (mActive) {
			mMapController.animateTo(geopoint);
		}
	}

	public boolean isActive() {
		return mActive;
	}

	public void setActive(boolean active) {
		mActive = active;
		if (mActive) {
			mServiceConnection.registerLocationUpdates(this);
		} else {
			mServiceConnection.unregisterLocationUpdates(this);
		}
	}

	@Override
	public void onLocationAvailability() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleDirectionUpdate(final float direction) {
		// TODO Auto-generated method stub

	}

}
