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

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class CompassListener implements SensorEventListener {

	private ServiceThread	mServiceThread	= null;

	public CompassListener(final ServiceThread serviceThread) {
		mServiceThread = serviceThread;
	}

	@Override
	public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(final SensorEvent event) {
		mServiceThread.updateDirection(event.values[0]);
	}
}