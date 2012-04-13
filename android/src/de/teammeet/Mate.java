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

import com.google.android.maps.GeoPoint;

public class Mate {

	private String mID = null; 
	private GeoPoint mLocation	= null;
	public float	mAccuracy	= 0;

	public Mate(final String id, final GeoPoint location, final float accuracy) {
		mID = id;
		mLocation = location;
		mAccuracy = accuracy;
	}
	
	public String getID() {
		return mID;
	}
	
	public GeoPoint getLocation() {
		return mLocation;
	}
	
	public void setLocation(GeoPoint location) {
		mLocation = location;
		mAccuracy = accuracy;
	}
}
