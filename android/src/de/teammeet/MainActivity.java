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

package de.teammeet;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

	private String	CLASS	= MainActivity.class.getSimpleName();

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Button b;

		b = (Button) findViewById(R.id.buttonShowMap);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View arg0) {
				startMapActivity();
			}
		});

		b = (Button) findViewById(R.id.buttonShowSettings);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View arg0) {
				startSettingsActivity();
			}
		});

		b = (Button) findViewById(R.id.buttonXMPP);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(final View arg0) {
				sendXMPPMessage();
			}
		});
	}

	protected void startMapActivity() {
		final Intent intent = new Intent(MainActivity.this, TeamMeetActivity.class);
		startActivity(intent);
	}

	protected void startSettingsActivity() {
		final Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
		startActivity(intent);
	}

	protected void sendXMPPMessage() {
		new Thread(new Runnable() {
			public void run() {

				// try {
				// SSLContext sc = SSLContext.getInstance("TLS");
				// TrustManagerFactory tmf =
				// TrustManagerFactory.getInstance(KeyManagerFactory
				// .getDefaultAlgorithm());
				// try {
				// sc.init(null, tmf.getTrustManagers(), new SecureRandom());
				// } catch (KeyManagementException e) {
				// Log.e(CLASS, "KeyManagementException:\n" + e.getMessage());
				// e.printStackTrace();
				// }
				// } catch (NoSuchAlgorithmException e1) {
				// Log.e(CLASS, "NoSuchAlgorithmException:\n" +
				// e1.getMessage());
				// e1.printStackTrace();
				// }

				ConnectionConfiguration config = new ConnectionConfiguration("jabber.no");
				config.setSelfSignedCertificateEnabled(true);
				config.setDebuggerEnabled(true);
				config.setSecurityMode(SecurityMode.disabled);
				XMPPConnection xmpp = new XMPPConnection(config);
				try {
					xmpp.connect();

					xmpp.login("teammeettest", "teammeettestpass");

					Roster roster = xmpp.getRoster();

					for (RosterEntry r : roster.getEntries()) {
						Log.d(CLASS, r.getUser());
					}

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					MultiUserChat muc = new MultiUserChat(xmpp, "teammeettestroom@conference.jabber.org");
					// muc.create("ichbins");
					// muc.sendConfigurationForm(new Form(Form.TYPE_SUBMIT));
					muc.join("ichbins");
					muc.invite("XXX@jabber.org", "hier");
					muc.sendMessage("blablabla");

					ChatManager chatManager = xmpp.getChatManager();
					Chat chat = chatManager.createChat("XXX@jabber.org", new MessageListener() {

						@Override
						public void processMessage(Chat arg0, Message arg1) {
							// TODO Auto-generated method stub
							Log.d(CLASS, arg1.getBody());
						}
					});
					Message message = new Message();
					chat.sendMessage("huhu fuubaa");
				} catch (XMPPException e) {
					Log.e(CLASS, "Failed to login or send:\n" + e.getMessage());
					e.printStackTrace();
				}
			}
		}).start();
	}
}
