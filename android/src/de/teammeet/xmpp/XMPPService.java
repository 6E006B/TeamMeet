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
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.muc.MultiUserChat;

import com.google.android.maps.GeoPoint;

import de.teammeet.service.ServiceInterfaceImpl;

public class XMPPService {

	private XMPPConnection				mXMPP	= null;
	private String						mJID	= null;
	private String						mServer	= null;
	private Map<String, MultiUserChat>	groups	= null;

	public XMPPService() {
		ConfigureProviderManager.configureProviderManager();
		groups = new HashMap<String, MultiUserChat>();
	}

	public void connect(String jid, String server, String password) throws XMPPException {
		mJID = jid;
		mServer = server;

		ConnectionConfiguration config = new ConnectionConfiguration(server);
		config.setSelfSignedCertificateEnabled(true);
		config.setDebuggerEnabled(true);

		mXMPP = new XMPPConnection(config);
		mXMPP.connect();
		mXMPP.login(jid, password);
		MultiUserChat.addInvitationListener(mXMPP, new GroupInvitationListener());
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
		muc.create(mJID);
		muc.sendConfigurationForm(new Form(Form.TYPE_SUBMIT));
		muc.addMessageListener(new GroupMessageListener(serviceInterface));
		groups.put(groupName, muc);
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
