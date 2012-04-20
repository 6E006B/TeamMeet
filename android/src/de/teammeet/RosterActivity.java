package de.teammeet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.app.ExpandableListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.xmpp.XMPPService;


/**
 * Demonstrates expandable lists backed by a Simple Map-based adapter
 */
public class RosterActivity extends ExpandableListActivity implements RosterListener {
	private static final String CLASS = RosterActivity.class.getSimpleName();
	private static final String NAME = "name";
	private static final String AVAILABILITY = "avail";
	private static final String UNFILED_GROUP = "Unfiled contacts";

	private Roster mRoster = null;

	private IXMPPService mXMPPService = null;
	private XMPPServiceConnection mXMPPServiceConnection = new XMPPServiceConnection();
	private List<List<Map<String, String>>> expandableChildren;
	private SimpleExpandableListAdapter mAdapter;

	private class XMPPServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(CLASS, "RosterActivity.XMPPServiceConnection.onServiceConnected('" + className + "')");
			mXMPPService = ((XMPPService.LocalBinder) binder).getService();
			
			if (mRoster == null) {
				try {
					mRoster = mXMPPService.getRoster();
					mRoster.addRosterListener(RosterActivity.this);
				} catch (XMPPException e) {
					e.printStackTrace();
					String problem = String.format("Could not fetch roster: %s", e.getMessage());
					Log.e(CLASS, problem);
					Toast.makeText(getApplicationContext(), problem, 5);
				}
				
				fillExpandableList(mRoster);
			}

		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			Log.d(CLASS, "RosterActivity.XMPPServiceConnection.onServiceDisconnected('" + className + "')");
			mXMPPService = null;
		}
	};

	private class ExpandableContactEntry {
		protected Map<String, String> mGroup = null;
		protected List<Map<String, String>> mChildren = null;
	
		public ExpandableContactEntry(String groupName, Collection<RosterEntry> contacts, Roster roster) {
			mGroup = new HashMap<String, String>();
			mChildren = new ArrayList<Map<String, String>>();
			
			mGroup.put(NAME, groupName);

			for (RosterEntry contact : contacts) {
				Map<String, String> newChild = new HashMap<String, String>();
				String jid = contact.getUser();
				newChild.put(NAME, jid);
				newChild.put(AVAILABILITY, roster.getPresence(jid).toString());
				mChildren.add(newChild);
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS, "onCreate(): started roster activity");
	}

	@Override
	protected void onResume() {
		super.onResume();

		Log.d(CLASS, "RosterActivity.onResume()");

		// create the services (if they aren't already running)
		final Intent xmppIntent = new Intent(getApplicationContext(), XMPPService.class);
		startService(xmppIntent);

		Log.d(CLASS, "started XMPP service");

		// now connect to the services
		boolean bindSuccess = bindService(xmppIntent, mXMPPServiceConnection, 0);
		if (bindSuccess) {
			Log.d(CLASS, "onResume(): bind to XMPP service succeeded");
		} else {
			Log.e(CLASS, "onResume(): bind to XMPP service failed");
			Toast.makeText(getApplicationContext(), "Couldn't connect to XMPP service.", 3);
		}
	}

	@Override
	protected void onPause() {
		if (mXMPPServiceConnection != null) {
			unbindService(mXMPPServiceConnection);
		}
		mXMPPService = null;
		super.onPause();
	}
	
	/**
	 * Fill the contact list with data from the roster.
	 * 
	 * @param roster The roster containing all contact information
	 */
	private void fillExpandableList(Roster roster) {

		ExpandableContactEntry newEntry = null;
		List<Map<String, String>> expandableGroups = new ArrayList<Map<String, String>>();
		expandableChildren = new ArrayList<List<Map<String, String>>>();
	
		for (RosterGroup group : roster.getGroups()) {
			newEntry = new ExpandableContactEntry(group.getName(), group.getEntries(), roster);
			expandableGroups.add(newEntry.mGroup);
			expandableChildren.add(newEntry.mChildren);
			
		}

		if (roster.getUnfiledEntryCount() > 0) {
			newEntry = new ExpandableContactEntry(UNFILED_GROUP, roster.getUnfiledEntries(), roster);
			expandableGroups.add(newEntry.mGroup);
			expandableChildren.add(newEntry.mChildren);
		}
		
		mAdapter = new SimpleExpandableListAdapter(
				this,
				expandableGroups,
				android.R.layout.simple_expandable_list_item_1,
				new String[] { NAME },
				new int[] { android.R.id.text1},
				expandableChildren,
				android.R.layout.simple_expandable_list_item_2,
				new String[] { NAME, AVAILABILITY },
				new int[] { android.R.id.text1, android.R.id.text2}
				);
		setListAdapter(mAdapter);
	}

	@Override
	public void entriesAdded(Collection<String> arg0) {
		Log.d(CLASS, "Entries have been added to the roster. No action implemented");
	}

	@Override
	public void entriesDeleted(Collection<String> arg0) {
		Log.d(CLASS, "Entries have been deleted from the roster. No action implemented");
	}

	@Override
	public void entriesUpdated(Collection<String> arg0) {
		Log.d(CLASS, "Entries have been updated in the roster. No action implemented");
	}

	@Override
	public void presenceChanged(Presence presence) {
		String contact = StringUtils.parseBareAddress(presence.getFrom());
		String groupName = UNFILED_GROUP;
		RosterEntry entry = mRoster.getEntry(contact);
		if (entry != null) {
			groupName = ((RosterGroup)entry.getGroups().toArray()[0]).getName();
		} else {
			Log.d(CLASS, String.format("Couldn't get roster entry for '%s'", contact));
		}
		Log.d(CLASS, String.format("presence of '%s' in group '%s' has changed in the roster.", contact, groupName));

		expandableChildren.get(0).get(1).put(AVAILABILITY, "foo!!");
		mAdapter.notifyDataSetChanged();
	}
}