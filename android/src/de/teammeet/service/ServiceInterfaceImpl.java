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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.jivesoftware.smack.XMPPException;

import android.os.Binder;
import android.util.Log;

import com.google.android.maps.GeoPoint;

import de.teammeet.Mate;
import de.teammeet.interfaces.ILocationUpdateRecipient;
import de.teammeet.interfaces.IMatesUpdateRecipient;
import de.teammeet.interfaces.IService;
import de.teammeet.xmpp.XMPPService;

public class ServiceInterfaceImpl extends Binder implements IService {

	private static final String						CLASS				= ServiceInterfaceImpl.class
																				.getSimpleName();
	private final ReentrantLock						mLockMates			= new ReentrantLock();
	private final ReentrantLock						mLockLocation		= new ReentrantLock();
	private final List<ILocationUpdateRecipient>	mLocationRecipients	= new ArrayList<ILocationUpdateRecipient>();
	private final List<IMatesUpdateRecipient>		mMatesRecipients	= new ArrayList<IMatesUpdateRecipient>();
	private XMPPService								mXMPPService		= null;

	public ServiceInterfaceImpl(XMPPService xmppService) {
		mXMPPService = xmppService;
	}

	public void updateMate(final Mate mate) {
		acquireMatesLock();
		try {
			if (mate != null) {
				for (final IMatesUpdateRecipient object : mMatesRecipients) {
					object.handleMateUpdate(mate);
				}
			}
		} finally {
			releaseMatesLock();
		}
	}

	@Override
	public void registerMatesUpdates(final IMatesUpdateRecipient object) {
		// Log.e(CLASS, "registerMatesUpdates(" + object.getClass()
		// .getSimpleName() + ")");
		acquireMatesLock();
		try {
			mMatesRecipients.add(object);
		} finally {
			releaseMatesLock();
		}
	}

	@Override
	public void registerLocationUpdates(final ILocationUpdateRecipient object) {
		// Log.e(CLASS, "registerLocationUpdates(" + object.getClass()
		// .getSimpleName() + ")");
		acquireLocationLock();
		try {
			mLocationRecipients.add(object);
		} finally {
			releaseLocationLock();
		}
	}

	@Override
	public void unregisterMatesUpdates(final IMatesUpdateRecipient object) {
		// Log.e(CLASS, "unregisterMatesUpdates(" + object.getClass()
		// .getSimpleName() + ")");
		acquireMatesLock();
		try {
			mMatesRecipients.remove(object);
		} finally {
			releaseMatesLock();
		}
	}

	@Override
	public void unregisterLocationUpdates(final ILocationUpdateRecipient object) {
		// Log.e(CLASS, "unregisterLocationUpdates(" + object.getClass()
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

	private void acquireMatesLock() {
		mLockMates.lock();
	}

	private void releaseLocationLock() {
		mLockLocation.unlock();
	}

	private void releaseMatesLock() {
		mLockMates.unlock();
	}

	@Override
	public void connectXMPP(String userID, String server, String password) throws XMPPException {
		mXMPPService.connect(userID, server, password);
	}

	@Override
	public boolean isXMPPAuthenticated() {
		return mXMPPService.isAuthenticated();
	}

	@Override
	public void disconnectXMPP() {
		mXMPPService.disconnect();
	}
	
	@Override
	public void createGroup(String groupName) throws XMPPException {
		mXMPPService.createGroup(groupName, this);
	}

	@Override
	public void inviteContact(String contact, String groupName) {
		mXMPPService.invite(contact, groupName);
	}

	@Override
	public void setIndicator(GeoPoint location) throws XMPPException {
		mXMPPService.sendIndicator(location);
	}

	@Override
	public void deleteIndicator(GeoPoint location) {
		Log.d(CLASS, "deleteIndicator(" + location.toString() + ") not yet implemented");
	}

	public void sendLocation(GeoPoint mLocation, float accuracy) {
		Log.e(CLASS, "sendLocation() has no implementation!");
	}

}
