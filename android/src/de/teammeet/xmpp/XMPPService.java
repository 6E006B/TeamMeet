package de.teammeet.xmpp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.maps.GeoPoint;

import de.teammeet.Mate;
import de.teammeet.SettingsActivity;
import de.teammeet.interfaces.IMatesUpdateRecipient;
import de.teammeet.interfaces.IXMPPService;

public class XMPPService extends Service implements IXMPPService {

	private static final String					CLASS				= XMPPService.class.getSimpleName();

	private XMPPConnection						mXMPP				= null;
	private String								mUserID				= null;
	private String								mServer				= null;
	private Map<String, MultiUserChat>			groups				= null;

	private final ReentrantLock					mLockMates			= new ReentrantLock();
	private final ReentrantLock mLockGroups = new ReentrantLock();
	private final List<IMatesUpdateRecipient>	mMatesRecipients	= new ArrayList<IMatesUpdateRecipient>();

	private final IBinder						mBinder				= new LocalBinder();


	public class LocalBinder extends Binder {
		public XMPPService getService() {
			return XMPPService.this;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(CLASS, "XMPPService.onCreate()");
		ConfigureProviderManager.configureProviderManager();
		groups = new HashMap<String, MultiUserChat>();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
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
		new Thread(new Runnable() {
			@Override
			public void run() {
				disconnect();
			}
		}).start();
		super.onDestroy();
	}

	@Override
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
		// TODO: This is the default. Enable for production mode?!
		config.setCompressionEnabled(false);
		// config.setExpiredCertificatesCheckEnabled(false);
		// config.setSASLAuthenticationEnabled(false);
		// config.setNotMatchingDomainCheckEnabled(false);
		// config.setSecurityMode(SecurityMode.disabled);

		mXMPP = new XMPPConnection(config);
		mXMPP.connect();
		SASLAuthentication.supportSASLMechanism("PLAIN", 0);
		mXMPP.login(userID, password);
		SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
		MultiUserChat.addInvitationListener(mXMPP, new RoomInvitationListener(settings, this));
	}

	@Override
	public boolean isAuthenticated() {
		boolean authenticated = false;
		if (mXMPP != null) {
			authenticated = mXMPP.isAuthenticated();
		}
		Log.d(CLASS, "XMPPService.isAuthenticated() -> " + authenticated);
		return authenticated;
	}

	@Override
	public void disconnect() {
		Log.d(CLASS, "XMPPService.disconnect()");
		if (mXMPP != null) {
			mXMPP.disconnect();
		}
		stopSelf();
	}

	@Override
	public Map<String, List<String>> getContacts() throws XMPPException {
		Map<String, List<String>> contacts = new HashMap<String, List<String>>();
		if (mXMPP != null) {
			Roster roster = mXMPP.getRoster();
			Log.d(CLASS, String.format("Num of groups in roster: '%d'", roster.getGroupCount()));
			for (RosterGroup group : roster.getGroups()) {
				String groupName = group.getName();
				Log.d(CLASS, String.format("group: %s", groupName));
				List<String> groupContacts = new ArrayList<String>();
				for (RosterEntry contact : group.getEntries()) {
					groupContacts.add(contact.getUser());
				}
				contacts.put(groupName, groupContacts);
			}
		} else {
			// TODO: define better Exception
			throw new XMPPException("Connect before getting contacts!");
		}
		return contacts;
	}

	@Override
	public void addContact(String userID, String identifier) throws XMPPException {
		Roster roster = mXMPP.getRoster();
		roster.createEntry(userID, identifier, null);
	}

	@Override
	public void createRoom(String groupName, String conferenceServer) throws XMPPException {
		MultiUserChat muc = new MultiUserChat(mXMPP, String.format("%s@%s", groupName,
		                                                           conferenceServer));
		muc.create(mUserID);
		muc.sendConfigurationForm(new Form(Form.TYPE_SUBMIT));
		addRoom(groupName, muc);
	}

	@Override
	public void joinRoom(String roomName, String userID, String password, String conferenceServer)
			throws XMPPException {
		MultiUserChat muc = new MultiUserChat(mXMPP, conferenceServer);
		muc.join(userID, password);
		addRoom(roomName, muc);
	}

	private void addRoom(String roomName, MultiUserChat muc) {
		acquireGroupsLock();
		muc.addMessageListener(new RoomMessageListener(this));
		groups.put(roomName, muc);
		releaseGroupsLock();
	}

	private void removeRoom(String roomName) {
		acquireGroupsLock();
		groups.remove(roomName);
		//TODO find out if the GroupMessageHandler has to be removed
		// if there has to be an additional dict of handlers
		releaseGroupsLock();
	}

	@Override
	public void leaveRoom(String roomName) {
		acquireGroupsLock();
		MultiUserChat muc = groups.get(roomName);
		if (muc != null) {
			muc.leave();
			removeRoom(roomName);
		}
		releaseGroupsLock();
	}

	@Override
	public void destroyRoom(String roomName) throws XMPPException {
		acquireGroupsLock();
		MultiUserChat muc = groups.get(roomName);
		if (muc != null) {
			SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
			String userID = settings.getString(SettingsActivity.SETTING_XMPP_USER_ID, "");
			String server = settings.getString(SettingsActivity.SETTING_XMPP_SERVER, "");
			String alternateAddress = String.format("%s@%s", userID, server);
			muc.destroy("reason", alternateAddress);
			removeRoom(roomName);
		}
		releaseGroupsLock();
	}

	@Override
	public void invite(String contact, String groupName) {
		MultiUserChat muc = groups.get(groupName);
		muc.invite(contact, "reason");
		// TODO: there is an InvitationRejectionListener - maybe use it
	}

	@Override
	public void sendLocation(GeoPoint location, float accuracy) throws XMPPException {
		if (mXMPP != null) {
			if (mXMPP.isAuthenticated()) {
				Message message = new Message();
				GeolocPacketExtension geoloc = new GeolocPacketExtension(location.getLongitudeE6(),
				                                                         location.getLatitudeE6(),
				                                                         accuracy);
				message.addExtension(geoloc);
				sendAllGroups(message);
			} else {
				throw new XMPPException("Not authenticated.");
			}
		} else {
			throw new XMPPException("Not connected.");
		}
	}

	@Override
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

	@Override
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

	private void acquireMatesLock() {
		mLockMates.lock();
	}

	private void releaseMatesLock() {
		mLockMates.unlock();
	}

	private void acquireGroupsLock() {
		mLockGroups.lock();
	}

	private void releaseGroupsLock() {
		mLockGroups.unlock();
	}
}
