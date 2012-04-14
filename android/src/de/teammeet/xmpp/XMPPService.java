package de.teammeet.xmpp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.util.Log;

import com.google.android.maps.GeoPoint;

import de.teammeet.service.ServiceInterfaceImpl;

public class XMPPService {

	private static final String CLASS = XMPPService.class.getSimpleName();

	private XMPPConnection				mXMPP	= null;
	private String						mUserID	= null;
	private String						mServer	= null;
	private Map<String, MultiUserChat>	groups	= null;

	public XMPPService() {
		Log.d(CLASS, "XMPPService() created");
		ConfigureProviderManager.configureProviderManager();
		groups = new HashMap<String, MultiUserChat>();
	}

	public void connect(String userID, String server, String password) throws XMPPException {
		mUserID = userID;
		mServer = server;

		Log.d(CLASS, "XMPPService.connect('" + mUserID + "', '" + mServer + "')");
		ConnectionConfiguration config = new ConnectionConfiguration(server);
		config.setSelfSignedCertificateEnabled(true);
		config.setDebuggerEnabled(true);
		config.setCompressionEnabled(false);
//		config.setExpiredCertificatesCheckEnabled(false);
//		config.setSASLAuthenticationEnabled(false);
		config.setReconnectionAllowed(true);
//		config.setNotMatchingDomainCheckEnabled(false);
		config.setSecurityMode(SecurityMode.disabled);
		// This could be helpful to ensure a roster request after login (mandatory by XMPP)
		config.setRosterLoadedAtLogin(true); // TODO Check if this realy does it

		mXMPP = new XMPPConnection(config);
		mXMPP.connect();
		mXMPP.login(userID, password);
		MultiUserChat.addInvitationListener(mXMPP, new GroupInvitationListener());
	}

	public boolean isAuthenticated() {
		boolean authenticated = false;
		if (mXMPP != null) {
			authenticated = mXMPP.isAuthenticated();
		}
		Log.d(CLASS, "XMPPService.isAuthenticated() -> " + authenticated);
		return authenticated;
	}

	public void disconnect() {
		Log.d(CLASS, "XMPPService.disconnect()");
		if (mXMPP != null) {
			mXMPP.disconnect();
		}
	}

	public List<String> getContacts() throws Exception {
		List<String> contacts = new ArrayList<String>();
		if (mXMPP != null) {
			Roster roster = mXMPP.getRoster();
			for (RosterEntry r : roster.getEntries()) {
				contacts.add(r.getUser());
			}
		} else {
			// TODO: define better Exception
			throw new Exception("Connect before getting contacts!");
		}
		return contacts;
	}

	public void createGroup(String groupName, ServiceInterfaceImpl serviceInterface) throws XMPPException {
		MultiUserChat muc = new MultiUserChat(mXMPP, String.format("%s@conference.%s", groupName, mServer));
		muc.create(mUserID);
		muc.sendConfigurationForm(new Form(Form.TYPE_SUBMIT));
		muc.addMessageListener(new GroupMessageListener(serviceInterface));
		groups.put(groupName, muc);
	}

	public void invite(String contact, String groupName) {
		MultiUserChat muc = groups.get(groupName);
		muc.invite(contact, "reason");
		//TODO: there is an InvitationRejectionListener - maybe use it
	}

	public void sendLocation(GeoPoint location, float accuracy) throws XMPPException {
		Message message = new Message();
		GeolocPacketExtension geoloc = new GeolocPacketExtension(location.getLatitudeE6(),
				location.getLongitudeE6(), accuracy);
		message.addExtension(geoloc);
		sendAllGroups(message);
	}

	public void sendIndicator(GeoPoint location) throws XMPPException {
		Message message = new Message();
		IndicatorPacketExtension indication = new IndicatorPacketExtension(location.getLatitudeE6(),
				location.getLongitudeE6());
		message.addExtension(indication);
		sendAllGroups(message);
	}

	private void sendAllGroups(Message message) throws XMPPException {
		for (MultiUserChat muc : groups.values()) {
			muc.sendMessage(message);
		}
	}
}
