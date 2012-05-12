package de.teammeet.services.xmpp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MyLocationOverlay;

import de.teammeet.R;
import de.teammeet.activities.chat.Chat;
import de.teammeet.activities.chat.ChatsActivity;
import de.teammeet.activities.roster.RosterActivity;
import de.teammeet.activities.teams.Mate;
import de.teammeet.helper.ChatOpenHelper;
import de.teammeet.interfaces.IChatMessageHandler;
import de.teammeet.interfaces.IGroupMessageHandler;
import de.teammeet.interfaces.IInvitationHandler;
import de.teammeet.interfaces.IMatesUpdateRecipient;
import de.teammeet.interfaces.IXMPPService;

public class XMPPService extends Service implements IXMPPService {

	private static final String CLASS = XMPPService.class.getSimpleName();

	public static final String TYPE = "type";
	public static final String ROOM = "room";
	public static final String INVITER = "inviter";
	public static final String REASON = "reason";
	public static final String PASSWORD = "password";
	public static final String FROM = "from";
	public static final String GROUP = "group";
	public static final String SENDER = null;

	public static final int TYPE_NONE = 0;
	public static final int TYPE_JOIN = 1;

	private static final int NOTIFICATION_XMPP_SERVICE_ID = 0;
	private static final int NOTIFICATION_GROUP_INVITATION_ID = 1;
	private static final int NOTIFICATION_GROUP_CHAT_MESSAGE_ID = 2;
	private static final int NOTIFICATION_CHAT_MESSAGE_ID = 3;

	private XMPPConnection mXMPP = null;
	private String mUserID = null;
	private String mServer = null;
	private Map<String, MultiUserChat> mRooms = null;
	private RoomInvitationListener mRoomInvitationListener = null;
	private ChatMessageListener mChatMessageListener = null;

	private final ReentrantLock mLockMates = new ReentrantLock();
	private final ReentrantLock mLockGroups = new ReentrantLock();
	private final ReentrantLock mLockInvitations = new ReentrantLock();
	private final ReentrantLock mLockGroupMessages = new ReentrantLock();
	private final ReentrantLock mLockChatMessages = new ReentrantLock();

	private final List<IMatesUpdateRecipient> mMatesRecipients =
			new ArrayList<IMatesUpdateRecipient>();
	private final List<IInvitationHandler> mInvitationHandlers =
			new ArrayList<IInvitationHandler>();
	private final List<IGroupMessageHandler> mGroupMessageHandlers =
			new ArrayList<IGroupMessageHandler>();
	private final List<IChatMessageHandler> mChatMessageHandlers =
			new ArrayList<IChatMessageHandler>();

	private final IBinder mBinder = new LocalBinder();
	private int mBindCounter = 0;

	private TimerTask mTimerTask = null;
	private ChatOpenHelper mChatDatabase = null;
	private MyLocationOverlay mLocationOverlay = null;

	private NotificationCompat.Builder mServiceNotificationBuilder;
	private NotificationCompat.Builder mInvitationNotificationBuilder;

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
		mChatDatabase = new ChatOpenHelper(this);
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
		if (mChatMessageListener != null && mXMPP != null) {
			mXMPP.removePacketListener(mChatMessageListener);
			mXMPP.removePacketSendingListener(mChatMessageListener);
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				disconnect();
			}
		}).start();
		
		Intent bcastConnected = new Intent();
		bcastConnected.addCategory(getString(R.string.broadcast_connection_state));
		bcastConnected.setAction(getString(R.string.broadcast_connected));
		removeStickyBroadcast(bcastConnected);
		
		Intent bcastDisconnected = new Intent();
		bcastDisconnected.addCategory(getString(R.string.broadcast_connection_state));
		bcastDisconnected.setAction(getString(R.string.broadcast_disconnected));
		removeStickyBroadcast(bcastDisconnected);

		super.onDestroy();
	}

	@Override
	public void connect(String userID, String server, String password) throws XMPPException {
		mRooms = new HashMap<String, MultiUserChat>();
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

		mChatMessageListener = new ChatMessageListener(this);
		final MessageTypeFilter chatMessageFilter = new MessageTypeFilter(Message.Type.chat);
		mXMPP.addPacketListener(mChatMessageListener, chatMessageFilter);
		mXMPP.addPacketSendingListener(mChatMessageListener, chatMessageFilter);
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
			mXMPP.disconnect();
			if (mRoomInvitationListener != null) {
				MultiUserChat.removeInvitationListener(mXMPP, mRoomInvitationListener);
			}
		}
		removeNotifications();
		mRooms = null;
		mRoomInvitationListener = null;
		mXMPP = null;
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
		final String group = String.format("%s@%s", groupName, conferenceServer);
		MultiUserChat muc = new MultiUserChat(mXMPP, group);
		muc.create(mUserID);
		muc.sendConfigurationForm(new Form(Form.TYPE_SUBMIT));
		addRoom(group, muc);
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
		mRooms.put(room, muc);
		releaseGroupsLock();
	}

	private void removeRoom(String roomName) {
		acquireGroupsLock();
		mRooms.remove(roomName);
		//TODO find out if the GroupMessageHandler has to be removed
		// if there has to be an additional dict of handlers
		releaseGroupsLock();
	}

	@Override
	public void leaveRoom(String roomName) {
		acquireGroupsLock();
		MultiUserChat muc = mRooms.get(roomName);
		if (muc != null) {
			muc.leave();
			removeRoom(roomName);
		}
		releaseGroupsLock();
	}

	@Override
	public void destroyRoom(String roomName) throws XMPPException {
		acquireGroupsLock();
		MultiUserChat muc = mRooms.get(roomName);
		if (muc != null) {
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			String userID =
					settings.getString(getString(R.string.preference_user_id_key), "");
			String server =
					settings.getString(getString(R.string.preference_server_key), "");
			String alternateAddress = String.format("%s@%s", userID, server);
			muc.destroy("reason", alternateAddress);
			removeRoom(roomName);
		}
		releaseGroupsLock();
	}

	@Override
	public Set<String> getRooms() {
		return mRooms.keySet();
	}
	
	@Override
	public Iterator<String> getOccupants(String room) throws XMPPException {
		MultiUserChat muc = mRooms.get(room);
		Iterator<String> occupants;
		if (muc != null) {
			Log.d(CLASS, String.format("My nick in room is '%s'", muc.getNickname()));
			occupants = muc.getOccupants();
		} else {
			throw new XMPPException(String.format("No such room '%s'", room));
		}
		return occupants;
	}
	
	@Override
	public String getNickname(String room) throws XMPPException {
		MultiUserChat muc = mRooms.get(room);
		String nick;
		if (muc != null) {
			nick = muc.getNickname();
		} else {
			throw new XMPPException(String.format("No such room '%s'", room));
		}
		return nick;
	}

	@Override
	public void invite(String contact, String roomName) throws XMPPException {
		MultiUserChat muc = mRooms.get(roomName);
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
		if (mLocationOverlay != null) {
			mLocationOverlay.disableMyLocation();
		}
		mLocationOverlay = locationOverlay;
		locationOverlay.enableMyLocation();
		final int timeout = getResources().getInteger(R.integer.location_message_delay);
		final Timer timer = new Timer(CLASS, true);
		mTimerTask = new TimerTask() {
			@Override
			public void run() {
				GeoPoint location = locationOverlay.getMyLocation();
				if (location != null) {
					Location lastFix = locationOverlay.getLastFix();
					float accuracy = -1;
					if (lastFix != null) {
						accuracy = lastFix.getAccuracy();
					}
					try {
						sendLocation(location, (int)accuracy);
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
		if (mLocationOverlay != null) {
			mLocationOverlay.disableMyLocation();
		}
	}

	@Override
	public void sendLocation(GeoPoint location, int accuracy) throws XMPPException {
		if (mXMPP != null) {
			if (mXMPP.isAuthenticated()) {
				Log.d(CLASS, String.format("sending new location: %s", location.toString()));
				Message message = new Message();
				MatePacket matePacket = new MatePacket(location.getLongitudeE6(),
				                                       location.getLatitudeE6(),
				                                       accuracy);
				TeamMeetPacketExtension teamMeetPacket = new TeamMeetPacketExtension(matePacket,
				                                                                     null);
				message.addExtension(teamMeetPacket);
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
	public void sendIndicator(GeoPoint location, String info) throws XMPPException {
		if (mXMPP != null) {
			if (mXMPP.isAuthenticated()) {
				Message message = new Message();
				IndicatorPacket indicatorPacket = new IndicatorPacket(location.getLatitudeE6(),
				 						                             location.getLongitudeE6(),
										                             info);
				TeamMeetPacketExtension teamMeetPacket =
						new TeamMeetPacketExtension(null, indicatorPacket);
				message.addExtension(teamMeetPacket);
				message.addBody("", "");
				sendAllGroups(message);
			} else {
				throw new XMPPException("Not authenticated.");
			}
		} else {
			throw new XMPPException("Not connected.");
		}
	}

	public void broadcastIndicator(IndicatorPacket indicatorPacket) {
		final int lon = indicatorPacket.getLongitude();
		final int lat = indicatorPacket.getLatitude();
		final String info = indicatorPacket.getInfo();
		final boolean remove = indicatorPacket.isRemove();

		Intent intent = new Intent(getString(R.string.broadcast_action_indicator));
		intent.addCategory(getString(R.string.broadcast_category_location));
		intent.setData(Uri.parse(String.format("location:%d/%d", lon, lat)));
		intent.putExtra(TeamMeetPacketExtension.LON, lon);
		intent.putExtra(TeamMeetPacketExtension.LAT, lat);
		intent.putExtra(TeamMeetPacketExtension.INFO, info);
		intent.putExtra(TeamMeetPacketExtension.REMOVE, remove);
		if (remove) {
			removeStickyBroadcast(intent);
			Intent removeIntent = (Intent) intent.clone();
			removeIntent.setAction(getString(R.string.broadcast_action_indicator_remove));
			sendBroadcast(removeIntent);
		} else {
			sendStickyBroadcast(intent);
		}
	}

	public void removeAllIndicators() {
		//TODO: cleanup function to clear all sticky indicators on exit
		//TODO: we will need to have the same for specific groups only to be able to remove all
		//      sticky broadcasts once we leave the group (team)
	}

	private void sendAllGroups(Message message) throws XMPPException {
		message.setType(Message.Type.groupchat);
		for (MultiUserChat muc : mRooms.values()) {
			message.setTo(muc.getRoom());
			muc.sendMessage(message);
		}
	}

	public void sendChatMessage(String to, String message) {
		Message packet = new Message();
		packet.setBody(message);
		packet.setType(Message.Type.chat);
		packet.setTo(to);
		packet.setFrom(String.format("%s@%s", mUserID, mServer));
		mXMPP.sendPacket(packet);
	}

	public void sendToGroup(String group, String message) throws XMPPException {
		final MultiUserChat muc = mRooms.get(group);
		if (muc != null) {
			muc.sendMessage(message);
		} else {
			throw new XMPPException(String.format("Unknown group '%s'", group));
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
			if (!mMatesRecipients.contains(object)) {
				mMatesRecipients.add(object);
			}
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
		Log.d(CLASS, "XMPPService.showXMPPServiceNotification()");
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) getSystemService(ns);

		CharSequence title = getText(R.string.notification_service_title);
        CharSequence text = getText(R.string.notification_service_text);
		int icon = R.drawable.group_invitation_icon;
		CharSequence tickerText = String.format("%s %s", title, text);

		Intent notificationIntent = new Intent(this, RosterActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
		                                                        PendingIntent.FLAG_CANCEL_CURRENT);

		mServiceNotificationBuilder = new NotificationCompat.Builder(getApplicationContext());
		mServiceNotificationBuilder.setContentTitle(title);
		mServiceNotificationBuilder.setContentText(text);
		mServiceNotificationBuilder.setTicker(tickerText);
		mServiceNotificationBuilder.setSmallIcon(icon);
		mServiceNotificationBuilder.setOngoing(true);
		mServiceNotificationBuilder.setContentIntent(contentIntent);
		Notification notification = mServiceNotificationBuilder.getNotification();

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

		final CharSequence contentTitle = "Group Invitation received";
		final Intent notificationIntent = new Intent(this, RosterActivity.class);
		notificationIntent.putExtra(TYPE, TYPE_JOIN);
		notificationIntent.putExtra(ROOM, room);
		notificationIntent.putExtra(INVITER, inviter);
		notificationIntent.putExtra(REASON, reason);
		notificationIntent.putExtra(PASSWORD, password);
		notificationIntent.putExtra(FROM, message.getFrom());
		final PendingIntent contentIntent =
				PendingIntent.getActivity(this, 0, notificationIntent,
				                          PendingIntent.FLAG_UPDATE_CURRENT);

		Log.d(CLASS, "extra: " + notificationIntent.getExtras().toString());

		mInvitationNotificationBuilder = new NotificationCompat.Builder(getApplicationContext());
		mInvitationNotificationBuilder.setContentTitle(contentTitle);
		mInvitationNotificationBuilder.setContentText(tickerText);
		mInvitationNotificationBuilder.setTicker(tickerText);
		mInvitationNotificationBuilder.setSmallIcon(icon);
		mInvitationNotificationBuilder.setAutoCancel(true);
		mInvitationNotificationBuilder.setDefaults(Notification.DEFAULT_ALL);
		mInvitationNotificationBuilder.setContentIntent(contentIntent);
		Notification notification = mInvitationNotificationBuilder.getNotification();

		notificationManager.notify(NOTIFICATION_GROUP_INVITATION_ID, notification);
	}

	@Override
	public void registerInvitationHandler(IInvitationHandler object) {
		acquireInvitationsLock();
		try {
			if (!mInvitationHandlers.contains(object)) {
				mInvitationHandlers.add(object);
			}
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

	public void newGroupMessage(ChatMessage message) {
		mChatDatabase.addMessage(message);

		boolean handled = false;
		Log.d(CLASS, String.format("newGroupMessage('%s', '%s', '%d', '%s')",
		                           message.getFrom(), message.getTo(),
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

	private void notifyGroupMessage(ChatMessage message) {
		final String notificationText = String.format("%s (%s) : %s",
		                                              message.getFrom(),
		                                              message.getTo(),
		                                              message.getMessage());
		Log.d(CLASS, notificationText);

		final String ns = Context.NOTIFICATION_SERVICE;
		final NotificationManager notificationManager = (NotificationManager) getSystemService(ns);

		final int icon = R.drawable.group_invitation_icon;
		final CharSequence tickerText = String.format("New team message in %s", notificationText);
		final long when = System.currentTimeMillis();

		final Notification notification = new Notification(icon, tickerText, when);

		final CharSequence contentTitle = "Group chat message received";
		final Intent notificationIntent = new Intent(this, ChatsActivity.class);
		notificationIntent.putExtra(TYPE, Chat.TYPE_GROUP_CHAT);
		notificationIntent.putExtra(SENDER, message.getTo());
		notificationIntent.setAction(Long.toString(when));
		final PendingIntent contentIntent =
				PendingIntent.getActivity(this, 0, notificationIntent,
				                          PendingIntent.FLAG_UPDATE_CURRENT);

		Log.d(CLASS, "extra: " + notificationIntent.getExtras().toString());

		notification.setLatestEventInfo(this, contentTitle, notificationText, contentIntent);
	    notification.defaults = Notification.DEFAULT_ALL;
	    notification.flags |= Notification.FLAG_AUTO_CANCEL;

		notificationManager.notify(NOTIFICATION_GROUP_CHAT_MESSAGE_ID, notification);
	}

	@Override
	public void registerGroupMessageHandler(IGroupMessageHandler object) {
		acquireGroupMessageLock();
		try {
			if (!mGroupMessageHandlers.contains(object)) {
				mGroupMessageHandlers.add(object);
			}
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

	public void handleNewChatMessage(ChatMessage message) {
		mChatDatabase.addMessage(message);

		boolean handled = false;
		acquireChatMessageLock();
		try {
			for (final IChatMessageHandler object : mChatMessageHandlers) {
				handled |= object.handleMessage(message);
			}
		} finally {
			releaseChatMessageLock();
		}
		if (!handled) {
			Log.d(CLASS, "chat message has not been handled.");
			notifyNewChatMessage(message);
		}
	}

	private void notifyNewChatMessage(ChatMessage message) {
		final String notificationText = String.format("%s: %s",
		                                              message.getFrom(),
		                                              message.getMessage());
		Log.d(CLASS, notificationText);

		final String ns = Context.NOTIFICATION_SERVICE;
		final NotificationManager notificationManager = (NotificationManager) getSystemService(ns);

		final int icon = R.drawable.group_invitation_icon;
		final CharSequence tickerText = String.format("New message from %s", notificationText);
		final long when = System.currentTimeMillis();

		final Notification notification = new Notification(icon, tickerText, when);

		final CharSequence contentTitle = "Chat message received";
		final Intent notificationIntent = new Intent(this, ChatsActivity.class);
		notificationIntent.putExtra(TYPE, Chat.TYPE_NORMAL_CHAT);
		notificationIntent.putExtra(SENDER, message.getFrom());
		notificationIntent.setAction(Long.toString(when));
		final PendingIntent contentIntent =
				PendingIntent.getActivity(this, 0, notificationIntent,
				                          PendingIntent.FLAG_UPDATE_CURRENT);

		Log.d(CLASS, "extra: " + notificationIntent.getExtras().toString());

		notification.setLatestEventInfo(this, contentTitle, notificationText, contentIntent);
	    notification.defaults = Notification.DEFAULT_ALL;
	    notification.flags |= Notification.FLAG_AUTO_CANCEL;

		notificationManager.notify(NOTIFICATION_CHAT_MESSAGE_ID, notification);
	}

	@Override
	public void registerChatMessageHandler(IChatMessageHandler object) {
		acquireChatMessageLock();
		try {
			if (!mChatMessageHandlers.contains(object)) {
				mChatMessageHandlers.add(object);
			}
		} finally {
			releaseChatMessageLock();
		}
	}

	@Override
	public void unregisterChatMessageHandler(IChatMessageHandler object) {
		acquireChatMessageLock();
		try {
			mChatMessageHandlers.remove(object);
		} finally {
			releaseChatMessageLock();
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

	private void releaseChatMessageLock() {
		mLockChatMessages.unlock();
	}

	private void acquireChatMessageLock() {
		mLockChatMessages.lock();
	}
}