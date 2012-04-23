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
import org.jivesoftware.smack.packet.Presence;

import android.app.ExpandableListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;
import de.teammeet.interfaces.AsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.tasks.ConnectTask;
import de.teammeet.tasks.DisconnectTask;
import de.teammeet.tasks.FetchRosterTask;
import de.teammeet.tasks.InviteTask;
import de.teammeet.xmpp.XMPPService;


/**
 * Demonstrates expandable lists backed by a Simple Map-based adapter
 */
public class RosterActivity extends ExpandableListActivity implements RosterListener {
	private static final String CLASS = RosterActivity.class.getSimpleName();
	private static final String NAME = "name";
	private static final String AVAILABILITY = "avail";
	private static final String UNFILED_GROUP = "Unfiled contacts";

	private SimpleExpandableListAdapter mAdapter;
	private List<Map<String, String>> mExpandableGroups = new ArrayList<Map<String, String>>();
	private List<List<Map<String, String>>> mExpandableChildren = new ArrayList<List<Map<String, String>>>();

	private IXMPPService mXMPPService = null;
	private XMPPServiceConnection mXMPPServiceConnection = new XMPPServiceConnection();
	private Roster mRoster = null;


	private class XMPPServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(CLASS, "RosterActivity.XMPPServiceConnection.onServiceConnected('" + className + "')");
			mXMPPService = ((XMPPService.LocalBinder) binder).getService();
			
			if (mXMPPService.isAuthenticated() && mRoster == null) {
				new FetchRosterTask(mXMPPService, new FetchRosterHandler());
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
	
	private class ConnectHandler implements AsyncTaskCallback<Boolean> {
		@Override
		public void onTaskCompleted(Boolean result) {
			Log.d(CLASS, "connect task completed!!");
			new FetchRosterTask(mXMPPService, new FetchRosterHandler()).execute();
		}
	}

	private class FetchRosterHandler implements AsyncTaskCallback<Roster> {
		@Override
		public void onTaskCompleted(Roster roster) {
			if (roster != null) {
				mRoster = roster;
				Log.d(CLASS, "roster is " + mRoster);
				mRoster.addRosterListener(RosterActivity.this);
				fillExpandableList(mRoster);
				Log.d(CLASS, "list has been filled");
				
				mAdapter.notifyDataSetChanged();
			} else {
				//TODO Error handling. Inform user in dialog.
				final String error = "Could not fetch roster. Because!!";
				final Toast toast = Toast.makeText(RosterActivity.this, error, Toast.LENGTH_LONG);
				toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
				toast.show();
				Log.e(CLASS, error);
			}
		}
	}
	
	private class InviteMateHandler implements AsyncTaskCallback<String[]> {
		@Override
		public void onTaskCompleted(String[] params) {
			Toast.makeText(RosterActivity.this,
						   String.format("You invited %s to %s", params[0], params[1]),
						   Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS, "onCreate(): started roster activity");
		
		setContentView(R.layout.roster);
		
		mAdapter = new SimpleExpandableListAdapter(
				this,
				mExpandableGroups,
				android.R.layout.simple_expandable_list_item_1,
				new String[] { NAME },
				new int[] { android.R.id.text1},
				mExpandableChildren,
				android.R.layout.simple_expandable_list_item_2,
				new String[] { NAME, AVAILABILITY },
				new int[] { android.R.id.text1, android.R.id.text2}
				);
		
		setListAdapter(mAdapter);
		registerForContextMenu(getExpandableListView());
	}

	@Override
	protected void onResume() {
		super.onResume();

		Log.d(CLASS, "RosterActivity.onResume()");

		// create the service (if it isn't already running)
		final Intent xmppIntent = new Intent(getApplicationContext(), XMPPService.class);
		startService(xmppIntent);

		Log.d(CLASS, "started XMPP service");

		// now connect to the service
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
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.roster, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {

		switch (item.getItemId()) {
			case R.id.roster_menu_connect:
				Log.d(CLASS, "User clicked 'connect' in menu");
				performConnectButtonAction();
				break;

			case R.id.roster_menu_show_map:
				Log.d(CLASS, "User clicked 'map' in menu");
				startMapActvity();
				break;

			case R.id.roster_menu_form_team:
				Log.d(CLASS, "User clicked 'form team' in menu");
				break;

			case R.id.roster_menu_exit:
				Log.d(CLASS, "User clicked 'exit' in menu");
				performExit();
				break;

			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
									ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.roster_context, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Log.d(CLASS, String.format("Context item '%s' clicked", item.getTitleCondensed()));
		ExpandableListContextMenuInfo info = ((ExpandableListContextMenuInfo)item.getMenuInfo());
		Map<String, String> child = null;
		
		if(ExpandableListView.getPackedPositionType(info.packedPosition) ==
		   ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			int group_position = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			int child_position = ExpandableListView.getPackedPositionChild(info.packedPosition);
			child = (Map<String, String>) getExpandableListAdapter().getChild(group_position, child_position) ;
		} else {
			Log.e(CLASS, "Can't invite group of contacts");
			return super.onContextItemSelected(item);
		}
		
		switch(item.getItemId()) {
			case R.id.roster_list_context_invite:
				Log.d(CLASS, String.format("clicked contact '%s'", child.get(NAME)));
				SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
				String teamName = settings.getString(SettingsActivity.SETTING_XMPP_GROUP_NAME, "");
				new InviteTask(mXMPPService, new InviteMateHandler()).execute(child.get(NAME), teamName);
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}


	private void performExit() {
		mXMPPService.disconnect();
		final Intent intent = new Intent(getApplicationContext(), XMPPService.class);
		stopService(intent);
		finish();
	}

	private void startMapActvity() {
		final Intent intent = new Intent(getApplicationContext(), TeamMeetActivity.class);
		startActivity(intent);
	}

	private void performConnectButtonAction() {
		if (mXMPPService.isAuthenticated()) {
			new DisconnectTask((XMPPService)mXMPPService, null).execute();
		} else {
			new ConnectTask((XMPPService)mXMPPService, new ConnectHandler()).execute();
		}
	}

	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
		Log.d(CLASS, "preparing roster options menu");
		Resources res = getResources();
		MenuItem connect_menu = menu.findItem(R.id.roster_menu_connect);
		int connectTitle = R.string.roster_menu_connect;
		CharSequence connectTitleCondensed = res.getString(R.string.roster_menu_connect_condensed);
		if (mXMPPService != null && mXMPPService.isAuthenticated()) {
			Log.d(CLASS, "setting menu option to 'disconnect'");
			connectTitle = R.string.roster_menu_disconnect;
			connectTitleCondensed = res.getString(R.string.roster_menu_disconnect_condensed);
		}
		connect_menu.setTitle(connectTitle);
		connect_menu.setTitleCondensed(connectTitleCondensed);
		return true;
	}


	
	/**
	 * Fill the contact list with data from the roster.
	 * 
	 * @param roster The roster containing all contact information
	 */
	private void fillExpandableList(Roster roster) {
		ExpandableContactEntry newEntry = null;
		mExpandableGroups.clear();
		mExpandableChildren.clear();

		for (RosterGroup group : roster.getGroups()) {
			newEntry = new ExpandableContactEntry(group.getName(), group.getEntries(), roster);
			mExpandableGroups.add(newEntry.mGroup);
			mExpandableChildren.add(newEntry.mChildren);
		}

		if (roster.getUnfiledEntryCount() > 0) {
			newEntry = new ExpandableContactEntry(UNFILED_GROUP, roster.getUnfiledEntries(), roster);
			mExpandableGroups.add(newEntry.mGroup);
			mExpandableChildren.add(newEntry.mChildren);
		}
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
		/*
		String contact = StringUtils.parseBareAddress(presence.getFrom());
		String groupName = UNFILED_GROUP;
		RosterEntry entry = mRoster.getEntry(contact);
		if (entry != null) {
			groupName = ((RosterGroup)entry.getGroups().toArray()[0]).getName();
		} else {
			Log.d(CLASS, String.format("Couldn't get roster entry for '%s'", contact));
		}
		Log.d(CLASS, String.format("presence of '%s' in group '%s' has changed in the roster.", contact, groupName));
		*/
		
		getExpandableListView().post(new Runnable() {
			
			@Override
			public void run() {
				fillExpandableList(mRoster);
				mAdapter.notifyDataSetChanged();	
			}
		});
	}
}