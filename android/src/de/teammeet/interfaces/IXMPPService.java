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

package de.teammeet.interfaces;

import java.util.Collection;
import java.util.Set;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.Occupant;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MyLocationOverlay;

import de.teammeet.activities.teams.Mate;

public interface IXMPPService {

	void registerMatesUpdates(IMatesUpdateRecipient object);

	void unregisterMatesUpdates(IMatesUpdateRecipient object);

	void registerInvitationHandler(IInvitationHandler object);

	void unregisterInvitationHandler(IInvitationHandler object);

	void registerGroupMessageHandler(IGroupMessageHandler object);

	void unregisterGroupMessageHandler(IGroupMessageHandler object);

	void registerChatMessageHandler(IChatMessageHandler object);

	void unregisterChatMessageHandler(IChatMessageHandler object);

	void connect(String userID, String server, String password) throws XMPPException;

	boolean isAuthenticated();

	void disconnect();

	Roster getRoster() throws XMPPException;

	void addContact(String userID, String identifier) throws XMPPException;

	void createRoom(String groupName, String conferenceServer) throws XMPPException;

	void joinRoom(String room, String userID, String password) throws XMPPException;

	void leaveRoom(String roomName);

	void invite(String contact, String roomName) throws XMPPException;

	void sendChatMessage(String mSender, String sendText);

	void sendToGroup(String mGroup, String message) throws XMPPException;

	void startLocationTransmission(MyLocationOverlay locationOverlay);

	void stopLocationTransmission();

	void sendLocation(GeoPoint location, float accuracy) throws XMPPException;

	void sendIndicator(GeoPoint location) throws XMPPException;

	void updateMate(Mate mate);

	void destroyRoom(String roomName) throws XMPPException;

	Set<String> getRooms();

	Collection<Occupant> getParticipants(String room) throws XMPPException;
}
