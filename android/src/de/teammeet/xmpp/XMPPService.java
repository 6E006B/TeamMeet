package de.teammeet.xmpp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MyLocationOverlay;

import de.teammeet.MainActivity;
import de.teammeet.Mate;
import de.teammeet.R;
import de.teammeet.RosterActivity;
import de.teammeet.SettingsActivity;
import de.teammeet.interfaces.IGroupMessageHandler;
import de.teammeet.interfaces.IInvitationHandler;
import de.teammeet.interfaces.IMatesUpdateRecipient;
import de.teammeet.interfaces.IXMPPService;

public class XMPPService extends Service implements IXMPPService {

	private static final String					CLASS				= XMPPService.class.getSimpleName();

	public static final String TYPE = "type";
	public static final String ROOM = "room";
	public static final String INVITER = "inviter";
	public static final String REASON = "reason";
	public static final String PASSWORD = "password";
	public static final String FROM = "from";

	public static final int TYPE_NONE = 0;
	public static final int TYPE_JOIN = 1;

	private static final int NOTIFICATION_XMPP_SERVICE_ID = 0;
	private static final int NOTIFICATION_GROUP_INVITATION_ID = 1;

	private XMPPConnection						mXMPP				= null;
	private String								mUserID				= null;
	private String								mServer				= null;
	private Map<String, MultiUserChat>			mGroups				= null;
	private RoomInvitationListener mRoomInvitationListener = null;

	private final ReentrantLock mLockMates = new ReentrantLock();
	private final ReentrantLock mLockGroups = new ReentrantLock();
	private final ReentrantLock mLockInvitations = new ReentrantLock();
	private final ReentrantLock mLockGroupMessages = new ReentrantLock();


	private final List<IMatesUpdateRecipient> mMatesRecipients =
			new ArrayList<IMatesUpdateRecipient>();
	private final List<IInvitationHandler> mInvitationHandlers =
			new ArrayList<IInvitationHandler>();
	private final List<IGroupMessageHandler> mGroupMessageHandlers =
			new ArrayList<IGroupMessageHandler>();

	private final IBinder mBinder = new LocalBinder();
	private int mBindCounter = 0;

	private TimerTask mTimerTask;

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
	}

	@Override
	public IBinder onBind(Intent intent) {
		mBindCounter++;
		return mBinder;
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		mBindCounter--;
		Log.d(CLASS, "XMPPService.onUnbind() still " + mBindCounter + " connected");
		if (mBindCounter == 0 && !isAuthenticated()) {
			Log.d(CLASS, "XMPPService: No one bound and not connected -> selfdestruction!");
			stopSelf();
		}
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		Log.d(CLASS, "XMPPService.onDestroy()");
		removeNotifications();
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
		disconnect();
//			}
//		}).start();
		super.onDestroy();
	}

	@Override
	public void connect(String userID, String server, String password) throws XMPPException {
		mGroups = new HashMap<String, MultiUserChat>();
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
		mRoomInvitationListener  = new RoomInvitationListener(this);
		MultiUserChat.addInvitationListener(mXMPP, mRoomInvitationListener);

		showXMPPServiceNotification();
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
		stopLocationTransmission();
		if (mXMPP != null) {
			if (mRoomInvitationListener != null) {
				MultiUserChat.removeInvitationListener(mXMPP, mRoomInvitationListener);
			}
			mXMPP.disconnect();
		}
		mGroups = null;
		mRoomInvitationListener = null;
		mXMPP = null;
//		stopSelf();
	}

	@Override
	public Roster getRoster() throws XMPPException {
		Roster roster = null;
		if (mXMPP != null) {
			roster = mXMPP.getRoster();
		} else {
			// TODO: define better Exception
			throw new XMPPException("Connect before getting contacts!");
		}
		return roster;
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
	public void joinRoom(String room, String userID, String password) throws XMPPException {
		Log.d(CLASS, String.format("joinRoom('%s', '%s', '%s')",
		                           room, userID, password));
		MultiUserChat muc = new MultiUserChat(mXMPP, room);
		muc.join(userID, password);
		addRoom(room, muc);
	}

	private void addRoom(String room, MultiUserChat muc) {
		acquireGroupsLock();
		muc.addMessageListener(new RoomMessageListener(this, room));
		mGroups.put(room, muc);
		releaseGroupsLock();
	}

	private void removeRoom(String roomName) {
		acquireGroupsLock();
		mGroups.remove(roomName);
		//TODO find out if the GroupMessageHandler has to be removed
		// if there has to be an additional dict of handlers
		releaseGroupsLock();
	}

	@Override
	public void leaveRoom(String roomName) {
		acquireGroupsLock();
		MultiUserChat muc = mGroups.get(roomName);
		if (muc != null) {
			muc.leave();
			removeRoom(roomName);
		}
		releaseGroupsLock();
	}

	@Override
	public void destroyRoom(String roomName) throws XMPPException {
		acquireGroupsLock();
		MultiUserChat muc = mGroups.get(roomName);
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
	public void invite(String contact, String roomName) throws XMPPException {
		MultiUserChat muc = mGroups.get(roomName);
		if (muc != null) {
			muc.invite(contact, "reason");
		} else {
			throw new XMPPException(String.format("Cannot invite to non-existent MUC '%s'", roomName));
		}
		
		// TODO: there is an InvitationRejectionListener - maybe use it
	}

	@Override
	public void startLocationTransmission(final MyLocationOverlay locationOverlay) {
		if (mTimerTask != null) {
			mTimerTask.cancel();
		}
		final int timeout = getResources().getInteger(R.integer.location_message_delay);
		final Timer timer = new Timer(CLASS, true);
		mTimerTask = new TimerTask() {
			@Override
			public void run() {
				GeoPoint location = locationOverlay.getMyLocation();
				float accuracy = locationOverlay.getLastFix().getAccuracy();
				if (location != null) {
					try {
						sendLocation(location, accuracy);
					} catch (XMPPException e) {
						e.printStackTrace();
						Log.e(CLASS, "Error while sending location: " + e.toString());
					}
					Log.d(CLASS, "Location update to: " + location.toString());
				}
			}
		};
		timer.scheduleAtFixedRate(mTimerTask, timeout, timeout);
	}

	@Override
	public void stopLocationTransmission() {
		if (mTimerTask != null) {
			mTimerTask.cancel();
			mTimerTask = null;
		}
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
				message.addBody("", "");
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
		message.setType(Type.groupchat);
		for (MultiUserChat muc : mGroups.values()) {
			message.setTo(muc.getRoom());
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

	private void showXMPPServiceNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) getSystemService(ns);

		CharSequence title = getText(R.string.notification_service_title);
        CharSequence text = getText(R.string.notification_service_text);
		int icon = R.drawable.group_invitation_icon;
		CharSequence tickerText = String.format("%s %s", title, text);
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		
		Context context = getApplicationContext();
		Intent notificationIntent = new Intent(this, RosterActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
		                                                        PendingIntent.FLAG_CANCEL_CURRENT);

		notification.setLatestEventInfo(context, title, text, contentIntent);
		notification.flags |= Notification.FLAG_NO_CLEAR;

		notificationManager.notify(NOTIFICATION_XMPP_SERVICE_ID, notification);
	}

	private void removeNotifications() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		mNotificationManager.cancelAll();
	}

	public void newInvitation(Connection connection, String room, String inviter, String reason,
							  String password, Message message) {
		boolean handled = false;
		acquireInvitationsLock();
		try {
			for (final IInvitationHandler object : mInvitationHandlers) {
				handled |= object.handleInvitation(connection, room, inviter, reason, password, message);
			}
		} finally {
			releaseInvitationsLock();
		}
		if (!handled) {
			notifyNewInvitation(room, inviter, reason, password, message);
		}
	}

	private void notifyNewInvitation(String room, String inviter, String reason,
			  String password, Message message) {
		final String ns = Context.NOTIFICATION_SERVICE;
		final NotificationManager notificationManager = (NotificationManager) getSystemService(ns);

		final int icon = R.drawable.group_invitation_icon;
		final CharSequence tickerText = String.format("Invitation to '%s' from %s reason: '%s'",
		                                        room, inviter, reason);
		final long when = System.currentTimeMillis();

		final Notification notification = new Notification(icon, tickerText, when);

		final CharSequence contentTitle = "Group Invitation received";
		final Intent notificationIntent = new Intent(this, MainActivity.class);
		notificationIntent.putExtra(TYPE, TYPE_JOIN);
		notificationIntent.putExtra(ROOM, room);
		notificationIntent.putExtra(INVITER, inviter);
		notificationIntent.putExtra(REASON, reason);
		notificationIntent.putExtra(PASSWORD, password);
		notificationIntent.putExtra(FROM, message.getFrom());
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
		                            Intent.FLAG_ACTIVITY_CLEAR_TOP);
		final PendingIntent contentIntent =
				PendingIntent.getActivity(this, 0, notificationIntent, 0);

		Log.d(CLASS, "extra: " + notificationIntent.getExtras().toString());

		notification.setLatestEventInfo(this, contentTitle, tickerText, contentIntent);
	    notification.defaults = Notification.DEFAULT_ALL;
	    notification.flags |= Notification.FLAG_AUTO_CANCEL;

		notificationManager.notify(NOTIFICATION_GROUP_INVITATION_ID, notification);
	}

	@Override
	public void registerInvitationHandler(IInvitationHandler object) {
		acquireInvitationsLock();
		try {
			mInvitationHandlers.add(object);
		} finally {
			releaseInvitationsLock();
		}
	}

	@Override
	public void unregisterInvitationHandler(IInvitationHandler object) {
		acquireInvitationsLock();
		try {
			mInvitationHandlers.remove(object);
		} finally {
			releaseInvitationsLock();
		}
	}

	public void newGroupMessage(GroupChatMessage message) {
		boolean handled = false;
		Log.d(CLASS, String.format("newGroupMessage('%s', '%s', '%d', '%s')",
		                           message.getFrom(), message.getGroup(),
		                           message.getTimestamp(), message.getMessage()));
		acquireGroupMessageLock();
		try {
			for (IGroupMessageHandler handler : mGroupMessageHandlers) {
				handled |= handler.handleGroupMessage(message);
			}
		} finally {
			releaseGroupMessageLock();
		}
		if (!handled) {
			notifyGroupMessage(message);
		}
	}

	private void notifyGroupMessage(GroupChatMessage message) {
		String notificationText = String.format("%s (%s) : %s",
		                                        message.getFrom(),
		                                        message.getGroup(),
		                                        message.getMessage());
		Log.d(CLASS, notificationText);
		Toast toast = Toast.makeText(getApplicationContext(),
		                             notificationText,
		                             Toast.LENGTH_LONG);
		toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
		toast.show();
	}

	@Override
	public void registerGroupMessageHandler(IGroupMessageHandler object) {
		acquireGroupMessageLock();
		try {
			mGroupMessageHandlers.add(object);
		} finally {
			releaseGroupMessageLock();
		}
	}

	@Override
	public void unregisterGroupMessageHandler(IGroupMessageHandler object) {
		acquireGroupMessageLock();
		try {
			mGroupMessageHandlers.remove(object);
		} finally {
			releaseGroupMessageLock();
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

	private void releaseInvitationsLock() {
		mLockInvitations.unlock();
	}

	private void acquireInvitationsLock() {
		mLockInvitations.lock();
	}

	private void releaseGroupMessageLock() {
		mLockGroupMessages.unlock();
	}

	private void acquireGroupMessageLock() {
		mLockGroupMessages.lock();
	}
}