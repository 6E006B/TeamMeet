package de.teammeet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.packet.Presence;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;
import de.teammeet.interfaces.AsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.tasks.ConnectTask;
import de.teammeet.tasks.CreateGroupTask;
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
	private static final int DIALOG_FORM_TEAM_ID = 0x7e000000;
	//private static final int DIALOG_INVITE_MATE_ID = 0x7e000001;
	private static final int CONTEXT_MENU_INVITE_PARENT_ID = 0x7e000002;
	private static final int CONTEXT_MENU_INVITE_ROOM_ID = 0x7e000003;
	private SimpleExpandableListAdapter mAdapter;
	private List<Map<String, String>> mExpandableGroups = new ArrayList<Map<String, String>>();
	private List<List<Map<String, String>>> mExpandableChildren = new ArrayList<List<Map<String, String>>>();
	private ExpandableListContextMenuInfo mLastContextItemInfo;

	private IXMPPService mXMPPService = null;
	private XMPPServiceConnection mXMPPServiceConnection = new XMPPServiceConnection();
	private Roster mRoster = null;


	private class XMPPServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(CLASS, "RosterActivity.XMPPServiceConnection.onServiceConnected('" + className + "')");
			mXMPPService = ((XMPPService.LocalBinder) binder).getService();

			if (mXMPPService.isAuthenticated()) {
				new FetchRosterTask(mXMPPService, new FetchRosterHandler()).execute();
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
		public void onTaskCompleted(String[] connection_data) {
			String user_feedback;
			if (connection_data.length > 0) {
				user_feedback = String.format("You invited %s to %s", connection_data[0], connection_data[1]);
			} else {
				user_feedback = "Failed to invite contact to team!";
			}
			Toast.makeText(RosterActivity.this, user_feedback, Toast.LENGTH_LONG).show();

		}
	}
	
	private class FormTeamHandler implements AsyncTaskCallback<String[]> {
		@Override
		public void onTaskCompleted(String[] connection_data) {
			String user_feedback;
			if (connection_data.length > 0) {
				user_feedback = String.format("Founded team '%s'", connection_data[0]);
			} else {
				user_feedback = "Failed to form team!";
			}
			Toast.makeText(RosterActivity.this, user_feedback, Toast.LENGTH_LONG).show();
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
		mRoster = null;
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
				formTeamAction();
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
		//TODO Uncomment if you added static entries to an XML layout
		//MenuInflater inflater = getMenuInflater();
		//inflater.inflate(R.menu.roster_context, menu);
		Log.d(CLASS, "creating context menu");
		Set<String> rooms = mXMPPService.getRooms();
		if (!rooms.isEmpty()) {
			SubMenu inviteSubMenu = menu.addSubMenu(Menu.NONE, CONTEXT_MENU_INVITE_PARENT_ID,
													Menu.NONE, R.string.context_invite);
			for (String room : rooms) {
				Log.d(CLASS, "room: " + room);
				inviteSubMenu.add(Menu.NONE, CONTEXT_MENU_INVITE_ROOM_ID, Menu.NONE, room);
			}
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Log.d(CLASS, String.format("Context item '%s' clicked", item.getTitleCondensed()));

		switch(item.getItemId()) {
			case CONTEXT_MENU_INVITE_PARENT_ID:
				/* backup info, will need it when sub-menu item gets selected.
				 * cf http://code.google.com/p/android/issues/detail?id=7139.
				 */
				mLastContextItemInfo = ((ExpandableListContextMenuInfo)item.getMenuInfo());
				return true;
			case CONTEXT_MENU_INVITE_ROOM_ID:
				clickedInviteToTeam(item);
				return true;
			default:
				Log.d(CLASS, String.format("unhandeled item clicked: 0x%x", item.getItemId()));
				return super.onContextItemSelected(item);
		}
	}

	private void clickedInviteToTeam(MenuItem item) {
		String contact = getExpandableListChild(mLastContextItemInfo.packedPosition);
		String teamName = item.getTitle().toString();
		Log.d(CLASS, String.format("clicked contact '%s'", contact));
		AsyncTask<String, Void, String[]> inviteTask = new InviteTask(mXMPPService,
																	  new InviteMateHandler());
		inviteTask.execute(contact, teamName);
		// TODO Example code for dialog approach:
		//      dialog never shows
		/*Log.d(CLASS, "going to display invite mate dialog");
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				showDialog(DIALOG_INVITE_MATE_ID);
			}
		});
		Log.d(CLASS, "done showing invite mate dialog");
		*/
	}
	
	private String getExpandableListChild(long packedPosition) {
		final int group_position = ExpandableListView.getPackedPositionGroup(packedPosition);
		final int child_position = ExpandableListView.getPackedPositionChild(packedPosition);
		final ExpandableListAdapter adapter = getExpandableListAdapter();
		final Map<String, String> child = (Map<String, String>) adapter.getChild(group_position,
																		   child_position);
		return child.get(NAME);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Log.d(CLASS, "showing dialog" + id);
		Dialog dialog;
		switch(id) {
		case DIALOG_FORM_TEAM_ID:
			dialog = buildFormTeamDialog();
			break;
		/*case DIALOG_INVITE_MATE_ID:
			Log.d(CLASS, "showing invite mate dialog");
			dialog = buildInviteMateDialog();
		*/
		default:
			dialog = null;
		}
		return dialog;
	}

	private Dialog buildFormTeamDialog() {
		final LayoutInflater factory = LayoutInflater.from(this);
		final View formTeamView = factory.inflate(R.layout.form_team_dialog, null);
		final AlertDialog.Builder builder = new AlertDialog.Builder(RosterActivity.this);
		builder.setTitle(R.string.form_team_dialog_title);
		builder.setView(formTeamView);
		builder.setPositiveButton(R.string.button_create, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					EditText teamNameView = (EditText) formTeamView.findViewById(R.id.form_team_dialog_teamname);
					String teamName = teamNameView.getText().toString();
					Log.d(CLASS, String.format("chosen team name: %s", teamName));
					SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
					String conferenceSrv = settings.getString(SettingsActivity.SETTING_XMPP_CONFERENCE_SERVER, "");
					new CreateGroupTask(mXMPPService, new FormTeamHandler()).execute(teamName, conferenceSrv);
				}
			});
		return builder.create();
	}
	
	/*private Dialog buildInviteMateDialog() {
		final CharSequence[] items = {"Red", "Green", "Blue"};
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.invite_mate_dialog_title);
		builder.setItems(items, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		        Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_LONG).show();
		    }
		});
		Log.d(CLASS, "going to return invite mate dialog");
		return builder.create();
	}
	*/
	
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

	private void formTeamAction() {
		showDialog(DIALOG_FORM_TEAM_ID);
	}
	
	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
		Log.d(CLASS, "preparing roster options menu");

		MenuItem connectMenu = menu.findItem(R.id.roster_menu_connect);
		MenuItem formTeamMenu = menu.findItem(R.id.roster_menu_form_team);

		Resources res = getResources();
		int connectTitle = R.string.roster_menu_connect;
		CharSequence connectTitleCondensed = res.getString(R.string.roster_menu_connect_condensed);
		boolean enableFormTeam = false;
		
		if (mXMPPService != null && mXMPPService.isAuthenticated()) {
			Log.d(CLASS, "setting menu option to 'disconnect'");
			connectTitle = R.string.roster_menu_disconnect;
			connectTitleCondensed = res.getString(R.string.roster_menu_disconnect_condensed);
			enableFormTeam = true;
		}
		connectMenu.setTitle(connectTitle);
		connectMenu.setTitleCondensed(connectTitleCondensed);
		formTeamMenu.setEnabled(enableFormTeam);
		
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