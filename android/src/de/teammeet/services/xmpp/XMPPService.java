package de.teammeet.services.xmpp;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
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
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.Base64;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MyLocationOverlay;

import de.teammeet.R;
import de.teammeet.activities.roster.RosterActivity;
import de.teammeet.helper.BroadcastHelper;
import de.teammeet.helper.ChatOpenHelper;
import de.teammeet.helper.ToastHelper;
import de.teammeet.interfaces.IChatMessageHandler;
import de.teammeet.interfaces.IGroupMessageHandler;
import de.teammeet.interfaces.IInvitationHandler;
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
	public static final String SENDER = "sender";
	public static final String MESSAGE = "message";

	public static final int TYPE_NONE = 0;
	public static final int TYPE_JOIN = 1;

	private static final int NOTIFICATION_XMPP_SERVICE_ID = 0;
	private static final int NOTIFICATION_GROUP_INVITATION_ID = 1;
	private static final int NOTIFICATION_GROUP_CHAT_MESSAGE_ID = 2;
	private static final int NOTIFICATION_CHAT_MESSAGE_ID = 3;

	private static final String MUC_PASSWORDPROTECTED_FIELD = "muc#roomconfig_passwordprotectedroom";
	private static final String MUC_PASSWORD_FIELD = "muc#roomconfig_roomsecret";
	private static final String MUC_MEMBERSONLY_FIELD = "muc#roomconfig_membersonly";
	private static final String MUC_JIDRESOLVERS_FIELD = "muc#roomconfig_whois";
	private static final String MUC_ALLAFFILIATIONS_VALUE = "anyone";

	public static final String ACTION = "de.teammeet.xmpp_service.action";
	public static final String ACTION_CONNECT = "connect";

	private XMPPConnection mXMPP = null;
	private String mUserID = null;
	private String mServer = null;
	private Map<String, Team> mTeams = null;
	private RoomInvitationListener mRoomInvitationListener = null;
	private ChatMessageListener mChatMessageListener = null;
	private KeyExchangeListener mKeyExchangeRequestListener = null;
	private ConnectionReceiver mConnectionReceiver = null;

	private final ReentrantLock mLockGroups = new ReentrantLock();
	private final ReentrantLock mLockInvitations = new ReentrantLock();
	private final ReentrantLock mLockGroupMessages = new ReentrantLock();
	private final ReentrantLock mLockChatMessages = new ReentrantLock();

	private final List<IInvitationHandler> mInvitationHandlers =
			new ArrayList<IInvitationHandler>();
	private final List<IGroupMessageHandler> mGroupMessageHandlers =
			new ArrayList<IGroupMessageHandler>();
	private final List<IChatMessageHandler> mChatMessageHandlers =
			new ArrayList<IChatMessageHandler>();

	private final IBinder mBinder = new LocalBinder();

	private TimerTask mTimerTask = null;
	private ChatOpenHelper mChatDatabase = null;
	private MyLocationOverlay mLocationOverlay = null;
	private SecureRandom mRandomNumberGenerator = null;

	private NotificationCompat.Builder mServiceNotificationBuilder;
	private InvitationNotificationHandler mInvitationNotificationHandler;
	private GroupMessageNotificationHandler mGroupMessageNotificationHandler;
	private ChatMessageNotificationHandler mChatMessageNotificationHandler;

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
		ToastHelper.initialize(this);
		mChatDatabase = new ChatOpenHelper(this);
		mRandomNumberGenerator = new SecureRandom();
		mInvitationNotificationHandler =
				new InvitationNotificationHandler(this, R.drawable.ic_stat_notify_teammeet,
				                                  NOTIFICATION_GROUP_INVITATION_ID);
		mGroupMessageNotificationHandler =
				new GroupMessageNotificationHandler(this, R.drawable.ic_stat_notify_teammeet,
				                                    NOTIFICATION_GROUP_CHAT_MESSAGE_ID);
		mChatMessageNotificationHandler =
				new ChatMessageNotificationHandler(this, R.drawable.ic_stat_notify_teammeet,
				                                    NOTIFICATION_CHAT_MESSAGE_ID);
		showXMPPServiceNotification(false);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(CLASS, "XMPPService.onStartCommand()");
		if (intent.hasExtra(ACTION)) {
			if (intent.getStringExtra(ACTION).equals(ACTION_CONNECT)) {
				Log.d(CLASS, "contains connect extra");
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							connect();
						} catch (XMPPException e) {
							Log.e(CLASS, "Failed to login: " + e.getLocalizedMessage());
							e.printStackTrace();
						}
					}
				}).start();
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		Log.d(CLASS, "XMPPService.onDestroy()");
		if (mChatMessageListener != null && mXMPP != null) {
			mXMPP.removePacketListener(mChatMessageListener);
			mXMPP.removePacketSendingListener(mChatMessageListener);
		}
		disconnect();

		removeAllIndicators();

		Intent bcastConnected = new Intent();
		bcastConnected.addCategory(getString(R.string.broadcast_connection_state));
		bcastConnected.setAction(getString(R.string.broadcast_connected));
		removeStickyBroadcast(bcastConnected);

		Intent bcastDisconnected = new Intent();
		bcastDisconnected.addCategory(getString(R.string.broadcast_connection_state));
		bcastDisconnected.setAction(getString(R.string.broadcast_disconnected));
		removeStickyBroadcast(bcastDisconnected);

		removeNotifications();

		super.onDestroy();
	}

	@Override
	public void connect() throws XMPPException {
		mTeams = new HashMap<String, Team>();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		mUserID = settings.getString(getString(R.string.preference_user_id_key), "");
		mServer = settings.getString(getString(R.string.preference_server_key), "");
		String password =
				settings.getString(getString(R.string.preference_password_key), "");
		Log.d(CLASS, "XMPPService.connect() as " + mUserID);

		if (mXMPP != null) {
			Log.w(CLASS, "XMPPService.connect() : XMPPConnection not null -> disconnecting it");
			mXMPP.disconnect();
		}

		ConnectionConfiguration config = new ConnectionConfiguration(mServer);
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
		mXMPP.login(mUserID, password);
		mRoomInvitationListener  = new RoomInvitationListener(this);
		MultiUserChat.addInvitationListener(mXMPP, mRoomInvitationListener);

		mKeyExchangeRequestListener = new KeyExchangeListener(this);
		final PacketFilter keyExchangeRequestfilter = new AndFilter(new MessageTypeFilter(Message.Type.chat),
													  new PacketExtensionFilter(TeamMeetPacketExtension.NAMESPACE));
		mXMPP.addPacketListener(mKeyExchangeRequestListener, keyExchangeRequestfilter);
		mChatMessageListener = new ChatMessageListener(this);
		final MessageTypeFilter chatMessageFilter = new MessageTypeFilter(Message.Type.chat);
		mXMPP.addPacketListener(mChatMessageListener, chatMessageFilter);
		mXMPP.addPacketSendingListener(mChatMessageListener, chatMessageFilter);
		mXMPP.addConnectionListener(new ConnectionListener() {
			@Override
			public void reconnectionSuccessful() {
				BroadcastHelper.toggleConnectionStateBroadcast(XMPPService.this,
				                                               R.string.broadcast_disconnected,
				                                               R.string.broadcast_connected);
			}
			@Override
			public void reconnectionFailed(Exception arg0) {
				BroadcastHelper.toggleConnectionStateBroadcast(XMPPService.this,
															   R.string.broadcast_connected,
															   R.string.broadcast_disconnected);
			}
			@Override
			public void reconnectingIn(int arg0) {
				BroadcastHelper.toggleConnectionStateBroadcast(XMPPService.this,
															   R.string.broadcast_connected,
															   R.string.broadcast_disconnected);
			}
			@Override
			public void connectionClosedOnError(Exception arg0) {
				BroadcastHelper.toggleConnectionStateBroadcast(XMPPService.this,
															   R.string.broadcast_connected,
															   R.string.broadcast_disconnected);
			}
			@Override
			public void connectionClosed() {
				BroadcastHelper.toggleConnectionStateBroadcast(XMPPService.this,
															   R.string.broadcast_connected,
															   R.string.broadcast_disconnected);
			}
		});
		mConnectionReceiver = new ConnectionReceiver();
		registerReceiver(mConnectionReceiver,
		                 new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		showXMPPServiceNotification(true);
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
		if (mConnectionReceiver != null) {
			unregisterReceiver(mConnectionReceiver);
			mConnectionReceiver = null;
		}
		if (mXMPP != null) {
			if (mXMPP.isConnected()) {
				mXMPP.disconnect();
			}
			if (mRoomInvitationListener != null) {
				MultiUserChat.removeInvitationListener(mXMPP, mRoomInvitationListener);
			}
			showXMPPServiceNotification(false);
		}

		mUserID = null;
		mServer = null;
		mTeams = null;
		mRoomInvitationListener = null;
		mXMPP = null;
	}

	@Override
	public Roster getRoster() throws XMPPException {
		Roster roster = null;
		if (mXMPP != null) {
			roster = mXMPP.getRoster();
		} else {
			throw new XMPPException("Connect before getting contacts!");
		}
		return roster;
	}

	@Override
	public void createRoom(String groupName, String conferenceServer) throws XMPPException {
		final String group = String.format("%s@%s", groupName, conferenceServer);
		MultiUserChat muc = new MultiUserChat(mXMPP, group);
		muc.create(mUserID);

		Form configForm = createMUCConfig(muc.getConfigurationForm());

		String roomPassword = generateMUCPassword();
		configForm.setAnswer(MUC_PASSWORDPROTECTED_FIELD, true);
		configForm.setAnswer(MUC_PASSWORD_FIELD, roomPassword);
		Log.d(CLASS, String.format("Password for room '%s' is '%s'", groupName, roomPassword));

		configForm.setAnswer(MUC_MEMBERSONLY_FIELD, true);

		List<String> JIDResolverRoles = new ArrayList<String>();
		JIDResolverRoles.add(MUC_ALLAFFILIATIONS_VALUE);
		configForm.setAnswer(MUC_JIDRESOLVERS_FIELD, JIDResolverRoles);

		muc.sendConfigurationForm(configForm);

		Team team = new Team(group, muc);
		addTeam(group, team);
	}

	private Form createMUCConfig(Form template) {
		Form config = template.createAnswerForm();

		Iterator<FormField> templateFields = template.getFields();
		while (templateFields.hasNext()) {
			FormField field = (FormField) templateFields.next();
			if (!field.getType().equals(FormField.TYPE_HIDDEN) && field.getVariable() != null) {
				config.setDefaultAnswer(field.getVariable());
			}
		}

		if (config.getField(MUC_PASSWORDPROTECTED_FIELD) == null) {
			FormField passwordProtectedField = new FormField(MUC_PASSWORDPROTECTED_FIELD);
			passwordProtectedField.setType(FormField.TYPE_BOOLEAN);
			config.addField(passwordProtectedField);
		}

		return config;
	}

	private String generateMUCPassword() {
		byte[] roomKey = new byte[18]; // 18 bytes = 144 bits = 24 base64-chars
		mRandomNumberGenerator.nextBytes(roomKey);
		return Base64.encodeBytes(roomKey);
	}

	@Override
	public void joinTeam(String teamName, String userID, String password, String inviter) throws XMPPException {
		MultiUserChat muc = new MultiUserChat(mXMPP, teamName);
		Team team = new Team(teamName, muc);
		team.setInviter(inviter);
		addTeam(teamName, team);
		try {
			muc.join(userID, password);
		} catch (XMPPException error) {
			removeTeam(teamName);
			throw error;
		}
	}

	private void addTeam(final String teamName, Team team) {
		Log.d(CLASS, String.format("adding team '%s'", teamName));
		acquireTeamsLock();
		team.getRoom().addMessageListener(new RoomMessageListener(this, teamName));
		team.getRoom().addParticipantStatusListener(new TeamJoinListener(this, team));
		team.getRoom().addInvitationRejectionListener(new TeamJoinDeclinedListener(this, teamName));
		mTeams.put(teamName, team);
		releaseTeamsLock();
	}

	private void removeTeam(String teamName) {
		acquireTeamsLock();
		mTeams.remove(teamName);
		//TODO find out if the GroupMessageHandler has to be removed
		// if there has to be an additional dict of handlers
		releaseTeamsLock();
	}

	@Override
	public void leaveTeam(String teamName) throws XMPPException {
		acquireTeamsLock();
		Team team = mTeams.get(teamName);
		if (team != null) {
			team.getRoom().leave();
			removeTeam(teamName);
		} else {
			throw new XMPPException(String.format("No team '%s'", teamName));
		}
		releaseTeamsLock();
	}

	@Override
	public void destroyTeam(String teamName) throws XMPPException {
		acquireTeamsLock();
		Team team = mTeams.get(teamName);
		if (team != null) {
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			String userID =
					settings.getString(getString(R.string.preference_user_id_key), "");
			String server =
					settings.getString(getString(R.string.preference_server_key), "");
			String alternateAddress = String.format("%s@%s", userID, server);
			team.getRoom().destroy(getString(R.string.reason_team_destroy), alternateAddress);
			removeTeam(teamName);
		} else {
			throw new XMPPException(String.format("No team '%s'", teamName));
		}
		releaseTeamsLock();
	}

	@Override
	public String[] getGroups() {
		List<String> groupList = new ArrayList<String>();
		Collection<RosterGroup> rosterGroups = mXMPP.getRoster().getGroups();
		for (RosterGroup rosterGroup : rosterGroups) {
			groupList.add(rosterGroup.getName());
		}
		return groupList.toArray(new String[]{});
	}

	@Override
	public Set<String> getTeams() throws XMPPException {
		Set<String> teams = null;
		if (mTeams != null) {
			teams = mTeams.keySet();
		} else {
			throw new XMPPException("Connect before getting teams!");
		}
		return teams;
	}

	@Override
	public Team getTeam(String teamName) throws XMPPException {
		Team team = mTeams.get(teamName);
		if (team == null) {
			throw new XMPPException(String.format("No team '%s'", teamName));
		}
		return team;
	}
	
	@Override
	public Iterator<String> getMates(String teamName) throws XMPPException {
		Iterator<String> occupants;
		Team team = mTeams.get(teamName);
		if (team != null) {
			occupants = team.getRoom().getOccupants();
		} else {
			throw new XMPPException(String.format("No team '%s'", teamName));
		}
		return occupants;
	}

	@Override
	public String getNickname(String teamName) throws XMPPException {
		String nick;
		Team team = mTeams.get(teamName);
		if (team != null) {
			nick = team.getRoom().getNickname();
		} else {
			throw new XMPPException(String.format("No team '%s'", team));
		}
		return nick;
	}

	@Override
	public void addContact(String contact, String name, String group) throws XMPPException {
		Log.d(CLASS, "XMPPService.addContact()");
		Roster roster = mXMPP.getRoster();
		roster.createEntry(contact, name, new String[]{group});
	}

	@Override
	public void removeContact(String contact) throws XMPPException {
		Roster roster = mXMPP.getRoster();
		if (roster.contains(contact)) {
			RosterEntry contactEntry = roster.getEntry(contact);
			roster.removeEntry(contactEntry);
		} else {
			throw new XMPPException(String.format("No contact '%s'", contact));
		}
	}

	@Override
	public void invite(String contact, String teamName) throws XMPPException {
		Team team = mTeams.get(teamName);
		if (team != null) {
			team.getRoom().invite(contact, getString(R.string.reason_team_invite));
			team.addInvitee(contact);
		} else {
			throw new XMPPException(String.format("No team '%s'", teamName));
		}
	}

	@Override
	public void declineInvitation(String teamName, String inviter, String reason) throws XMPPException {
		if (mXMPP != null) {
		MultiUserChat.decline(mXMPP, teamName, inviter, reason);
		} else {
			throw new XMPPException("Not connected");
		}
	}

	@Override
	public void sendKey(String mate, String type, byte[] key, String team) throws XMPPException {
		if (mXMPP != null) {
			if (mXMPP.isAuthenticated()) {
				Log.d(CLASS, String.format("Sending key to '%s' in '%s'", mate, team));

				Message message = new Message();
				message.setType(Message.Type.chat);
				message.setTo(mate);
				CryptoPacket cryptoPacket = new CryptoPacket(type, key, team);
				TeamMeetPacketExtension teamMeetExt = new TeamMeetPacketExtension(null,
																				  null,
																				  cryptoPacket);
				message.addExtension(teamMeetExt);

				mXMPP.sendPacket(message);
			} else {
				throw new XMPPException("Not authenticated");
			}
		} else {
			throw new XMPPException("Not connected");
		}
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
				                                                                     null,
				                                                                     null);
				message.addExtension(teamMeetPacket);
				message.addBody("", "");
				sendAllTeams(message);
			} else {
				throw new XMPPException("Not authenticated.");
			}
		} else {
			throw new XMPPException("Not connected.");
		}
	}

	@Override
	public void sendIndicator(GeoPoint location, String info) throws XMPPException {
		sendIndicator(location, info, false);
	}

	@Override
	public void sendIndicator(GeoPoint location, String info, boolean remove) throws XMPPException {
		if (mXMPP != null) {
			if (mXMPP.isAuthenticated()) {
				Message message = new Message();
				IndicatorPacket indicatorPacket = new IndicatorPacket(location.getLatitudeE6(),
				 						                             location.getLongitudeE6(),
										                             info, remove);
				TeamMeetPacketExtension teamMeetPacket =
						new TeamMeetPacketExtension(null, indicatorPacket, null);
				message.addExtension(teamMeetPacket);
				message.addBody("", "");
				sendAllTeams(message);
			} else {
				throw new XMPPException("Not authenticated.");
			}
		} else {
			throw new XMPPException("Not connected.");
		}
	}

	public void broadcastIndicator(IndicatorPacket indicatorPacket, String team) {
		final int lon = indicatorPacket.getLongitude();
		final int lat = indicatorPacket.getLatitude();
		final String info = indicatorPacket.getInfo();
		final boolean remove = indicatorPacket.isRemove();

		Intent intent = new Intent(getString(R.string.broadcast_action_indicator));
		intent.addCategory(getString(R.string.broadcast_category_location));
		intent.setData(Uri.parse(String.format("location:%d/%d", lon, lat)));
		intent.putExtra(XMPPService.GROUP, team);
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
		//TODO: we will need to have the same for specific groups only to be able to remove all
		//      sticky broadcasts once we leave the group (team)
		BroadcastReceiver indicatorRemover = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {}
		};
		IntentFilter filter =
				new IntentFilter(getString(R.string.broadcast_action_indicator));
		filter.addCategory(getString(R.string.broadcast_category_location));
		filter.addDataScheme("location");
		Intent intent = registerReceiver(indicatorRemover, filter);
		while (intent != null) {
			removeStickyBroadcast(intent);
			unregisterReceiver(indicatorRemover);
			intent = registerReceiver(indicatorRemover, filter);
		}
		unregisterReceiver(indicatorRemover);
	}

	private void sendAllTeams(Message message) throws XMPPException {
		message.setType(Message.Type.groupchat);
		for (Team team : mTeams.values()) {
			MultiUserChat muc = team.getRoom();
			message.setTo(muc.getRoom());
			muc.sendMessage(message);
		}
	}

	public void sendChatMessage(String to, String message) {
		Message packet = new Message();
		packet.setBody(message);
		packet.setType(Message.Type.chat);
		packet.setTo(to);
		//TODO: Do we have/want to set from? May b0rk if we make a mistake, should be set by the server?
		packet.setFrom(String.format("%s@%s", mUserID, mServer));
		mXMPP.sendPacket(packet);
	}

	public void sendToTeam(String teamName, String message) throws XMPPException {
		final Team team = mTeams.get(teamName);
		if (team != null) {
			team.getRoom().sendMessage(message);
		} else {
			throw new XMPPException(String.format("No team '%s'", teamName));
		}
	}

	@Override
	public void updateMate(String from, int lon, int lat, int accuracy, String mTeam) {
		Intent intent = new Intent(getString(R.string.broadcast_action_teammate_update));
		intent.addCategory(getString(R.string.broadcast_category_location));
		intent.putExtra(XMPPService.GROUP, mTeam);
		intent.putExtra(TeamMeetPacketExtension.MATE, from);
		intent.putExtra(TeamMeetPacketExtension.LON, lon);
		intent.putExtra(TeamMeetPacketExtension.LAT, lat);
		intent.putExtra(TeamMeetPacketExtension.ACCURACY, accuracy);
		sendBroadcast(intent);
	}

	private void showXMPPServiceNotification(boolean connected) {
		Log.d(CLASS, "XMPPService.showXMPPServiceNotification()");
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notificationManager = (NotificationManager) getSystemService(ns);

		CharSequence title;
        CharSequence text;
		int icon;

		if (connected) {
			title = getText(R.string.notification_service_title_connected);
			text = getText(R.string.notification_service_text_connected);
			icon = R.drawable.ic_stat_notify_connected;
		} else {
			title = getText(R.string.notification_service_title_disconnected);
	        text = getText(R.string.notification_service_text_disconnected);
			icon = R.drawable.ic_stat_notify_teammeet;
		}
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
			final Bundle bundle = InvitationNotificationHandler.generateBundle(room, inviter,
			                                                                   reason, password);
			mInvitationNotificationHandler.newNotification(bundle);
		}
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
			final Bundle bundle =
					GroupMessageNotificationHandler.generateBundle(message.getTo(),
					                                               message.getFrom(),
					                                               message.getMessage());
			mGroupMessageNotificationHandler.newNotification(bundle);
		}
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
			final Bundle bundle =
					ChatMessageNotificationHandler.generateBundle(message.getFrom(),
					                                              message.getMessage());
			mChatMessageNotificationHandler.newNotification(bundle);
		}
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

	private void acquireTeamsLock() {
		mLockGroups.lock();
	}

	private void releaseTeamsLock() {
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

	private class ConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
            boolean isFailover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
			ConnectivityManager connectivityManager =
					(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo currentNetworkInfo = connectivityManager.getActiveNetworkInfo();
            NetworkInfo otherNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

            // do application-specific task(s) based on the current network state, such
            // as enabling queuing of HTTP requests when currentNetworkInfo is connected etc.
            Log.d(CLASS, "Connectivity changed: " + reason);
            if (isFailover) {
            	Log.d(CLASS, "IS_FAILOVER");
            } else {
            	Log.d(CLASS, "no failover");
            }
            Log.d(CLASS, "status: " + currentNetworkInfo);
        }
    };
}