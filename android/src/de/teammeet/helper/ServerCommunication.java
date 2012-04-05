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

import java.io.InvalidObjectException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.maps.GeoPoint;

import de.teammeet.Mate;
import de.teammeet.R;

public class ServerCommunication {
	private static final String	CLASS			= ServerCommunication.class.getSimpleName();
	public Resources			mResources		= null;
	public String				mServerUrl		= null;
	private Handler				mMessageHandler	= null;

	// private ToastDisposerSingleton m_toastSingleton = null;

	public ServerCommunication(final Handler messageHandler, final Resources res) {
		mMessageHandler = messageHandler;
		mResources = res;
		mServerUrl = mResources.getString(R.string.server_url);
	}

	@SuppressWarnings("unchecked")
	public Mate registerAtServer(final GeoPoint location) {
		// Log.e(CLASS, "ServerCommunication.registerAtServer(" + location
		// .toString() + ")");
		Mate mate = null;
		final HttpPostRequester httpPostRequester = new HttpPostRequester(mServerUrl);
		httpPostRequester.addPostParameter("action", "register");
		httpPostRequester.addPostParameter("latitude", String.valueOf(location.getLatitudeE6()));
		httpPostRequester.addPostParameter("longitude", String.valueOf(location.getLongitudeE6()));
		final Map resultMap = httpPostRequester.requestJSON();
		if (resultMap.containsKey("error")) {
			sendErrorToast((String) resultMap.get("error"));
			Log.e(CLASS, "Error on registration:\n" + resultMap.get("error"));
		} else {
			try {
				mate = convertMapToMate(resultMap);
			} catch (final InvalidObjectException e) {
				sendErrorToast("Error on registration:\n" + e.getMessage());
				Log.e(CLASS, "Error on registration:\n" + e.getMessage());
			}
		}
		return mate;
	}

	public Mate convertMapToMate(final Map<String, String> mateMap) throws InvalidObjectException {
		Mate mate = null;
		if (mateMap.containsKey("latitude") && mateMap.containsKey("longitude")) {
			final int latitude = Integer.parseInt(mateMap.get("latitude"));
			final int longitude = Integer.parseInt(mateMap.get("longitude"));
			final GeoPoint point = new GeoPoint(latitude, longitude);
			mate = new Mate(point);
		} else {
			throw new InvalidObjectException("Map didn't contain all needed keys to create a player object.");
		}
		return mate;
	}

	@SuppressWarnings("unchecked")
	public Set<Mate> sendPositionAndGetMatesFromServer(GeoPoint location) {
		Set<Mate> mates = new HashSet<Mate>();
		final HttpPostRequester httpPostRequester = new HttpPostRequester(mServerUrl);
		httpPostRequester.addPostParameter("action", "updatePositionAndGetMates");
		httpPostRequester.addPostParameter("latitude", String.valueOf(location.getLatitudeE6()));
		httpPostRequester.addPostParameter("longitude", String.valueOf(location.getLongitudeE6()));
		final Map json = httpPostRequester.requestJSON();

		if (json.containsKey("error")) {
			sendToast("Error while getting team mate positions:\n" + (String) json.get("error"));
		} else {
			mates = convertMatesMapToSet((Map<String, Map<String, String>>) json);
		}
		return mates;
	}

	public Set<Mate> convertMatesMapToSet(final Map<String, Map<String, String>> teamMatesMap) {
		final Set<Mate> matesSet = new HashSet<Mate>();
		for (final String id : teamMatesMap.keySet()) {
			final Map<String, String> mateMap = teamMatesMap.get(id);
			try {
				final Mate mate = convertMapToMate(mateMap);
				matesSet.add(mate);
			} catch (final InvalidObjectException e) {
				sendErrorToast("Error converting team mates map to player set:\n" + e.getMessage());
			}
		}
		return matesSet;
	}

	// private String generateMapToJson(final ArrayList<ArrayList<GeoPoint>>
	// map) {
	// String json = "[";
	// for (final ArrayList<GeoPoint> sector : map) {
	// json += "[";
	// for (final GeoPoint geoPoint : sector) {
	// json += "[" + geoPoint.getLatitudeE6() + "," + geoPoint.getLongitudeE6()
	// + "],";
	// }
	// json = json.substring(0, json.length() - 1);
	// json += "],";
	// }
	// json = json.substring(0, json.length() - 1);
	// json += "]";
	// return json;
	// }

	public void logout() {
		// Log.e(CLASS, "ServerCommunication.logout() called.");
		final HttpPostRequester httpPostRequester = new HttpPostRequester(mServerUrl);
		httpPostRequester.addPostParameter("action", "logout");
		httpPostRequester.requestJSON();
	}

	private void sendToast(final String message) {
		if (mMessageHandler != null) {
			final Message msg = new Message();
			final Bundle bundle = new Bundle();
			bundle.putString("toast", message);
			msg.setData(bundle);
			mMessageHandler.sendMessage(msg);
		}
	}

	private void sendErrorToast(final String message) {
		if (mMessageHandler != null) {
			final Message msg = new Message();
			final Bundle bundle = new Bundle();
			bundle.putString("error", message);
			msg.setData(bundle);
			mMessageHandler.sendMessage(msg);
		}
	}

}
