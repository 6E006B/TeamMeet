package de.teammeet.xmpp;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

public class XMPPService {

	private XMPPConnection	mXMPP	= null;

	public XMPPService() {
		ConfigureProviderManager.configureProviderManager();
	}

	public void connect(String jid, String server, String password) throws XMPPException {
		ConnectionConfiguration config = new ConnectionConfiguration(server);
		config.setSelfSignedCertificateEnabled(true);
		config.setDebuggerEnabled(true);

		mXMPP = new XMPPConnection(config);
		mXMPP.connect();
		mXMPP.login(jid, password);
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
}
