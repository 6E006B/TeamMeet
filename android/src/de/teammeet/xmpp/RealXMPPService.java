package de.teammeet.xmpp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.android.maps.GeoPoint;

import de.teammeet.service.ServiceInterfaceImpl;

public class RealXMPPService extends Service {

	private static final String			CLASS		= RealXMPPService.class.getSimpleName();

	public static final String			ACTION		= "action";
	public static final String			USER_ID		= "userID";
	public static final String			SERVER		= "server";
	public static final String			PASSWORD	= "password";

	public static final int				NO_ACTION	= -1;
	public static final int				CONNECT		= 0;

	private XMPPConnection				mXMPP		= null;
	private String						mUserID		= null;
	private String						mServer		= null;
	private Map<String, MultiUserChat>	groups		= null;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(CLASS, "RealXMPPService.onCreate()");
		ConfigureProviderManager.configureProviderManager();
		groups = new HashMap<String, MultiUserChat>();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int action = intent.getIntExtra(ACTION, NO_ACTION);
		switch (action) {
			case CONNECT:
				String userID = intent.getStringExtra(USER_ID);
				String server = intent.getStringExtra(SERVER);
				String password = intent.getStringExtra(PASSWORD);
				try {
					connect(userID, server, password);
				} catch (XMPPException e) {
					e.printStackTrace();
					Log.e(CLASS, "Failed to connect: " + e.toString());
				}
				break;
			case NO_ACTION:
			default:
				break;
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		disconnect();
		super.onDestroy();
	}

	public void connect(String userID, String server, String password) throws XMPPException {
		mUserID = userID;
		mServer = server;

		Log.d(CLASS, "XMPPService.connect('" + mUserID + "', '" + mServer + "')");

		if (mXMPP != null) {
			Log.w(CLASS, "XMPPService.connect() : XMPPConnection not null -> disconnecting it");
			mXMPP.disconnect();
		}

		ConnectionConfiguration config = new ConnectionConfiguration(server);
		config.setSelfSignedCertificateEnabled(true);
		config.setDebuggerEnabled(true);
		config.setCompressionEnabled(false);
		// config.setExpiredCertificatesCheckEnabled(false);
		// config.setSASLAuthenticationEnabled(false);
		config.setReconnectionAllowed(true);
		// config.setNotMatchingDomainCheckEnabled(false);
		// config.setSecurityMode(SecurityMode.disabled);
		// This could be helpful to ensure a roster request after login
		// (mandatory by XMPP)
		config.setRosterLoadedAtLogin(true); // TODO Check if this realy does it

		mXMPP = new XMPPConnection(config);
		mXMPP.connect();
		SASLAuthentication.supportSASLMechanism("PLAIN", 0);
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

	public void addContact(String userID, String identifier) throws XMPPException {
		Roster roster = mXMPP.getRoster();
		roster.createEntry(userID, identifier, null);
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
		// TODO: there is an InvitationRejectionListener - maybe use it
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
