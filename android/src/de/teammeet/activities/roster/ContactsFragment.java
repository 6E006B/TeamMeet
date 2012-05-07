package de.teammeet.activities.roster;

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

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.LinearLayout;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwoLineListItem;
import de.teammeet.R;
import de.teammeet.activities.chat.Chat;
import de.teammeet.activities.chat.ChatsActivity;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.services.xmpp.XMPPService;
import de.teammeet.tasks.BaseAsyncTaskCallback;
import de.teammeet.tasks.InviteTask;


/**
 * Demonstrates expandable lists backed by a Simple Map-based adapter
 */
public class ContactsFragment extends Fragment {
	private static final String CLASS = ContactsFragment.class.getSimpleName();
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

	private Roster mRoster = null;
	private ExpandableListView mContactsList;
	private RosterListener mRosterEventHandler;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS, String.format("Creating contacts fragment"));
		
		mAdapter = new SimpleExpandableListAdapter(
				getActivity(),
				mExpandableGroups,
				android.R.layout.simple_expandable_list_item_1,
				new String[] { NAME },
				new int[] { android.R.id.text1},
				mExpandableChildren,
				android.R.layout.simple_expandable_list_item_2,
				new String[] { NAME, AVAILABILITY },
				new int[] { android.R.id.text1, android.R.id.text2}
				);
		
		mRosterEventHandler = new RosterEventHandler();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		LinearLayout rootView = (LinearLayout) inflater.inflate(R.layout.contacts, container, false);
		mContactsList = (ExpandableListView) rootView.findViewById(R.id.contacts_list);
		mContactsList.setAdapter(mAdapter);
		mContactsList.setEmptyView(rootView.findViewById(R.id.contacts_empty));
		mContactsList.setOnChildClickListener(new OnChildClickListener() {
			
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
					int childPosition, long id) {
				Log.d(CLASS, String.format("onChildClick('%s', '%s', '%d', '%d', '%d')",
				                           parent.toString(), v.toString(), groupPosition,
				                           childPosition, id));
				TwoLineListItem listItem = (TwoLineListItem)v;
				TextView textView = (TextView) listItem.getChildAt(0);
				final String contact = textView.getText().toString();
				Log.d(CLASS, String.format("clicked on child: %s", contact));
				Intent intent = new Intent(getActivity(), ChatsActivity.class);
				intent.putExtra(XMPPService.TYPE, Chat.TYPE_NORMAL_CHAT);
				intent.putExtra(XMPPService.SENDER, contact);
				startActivity(intent);
				//return super.onChildClick(parent, v, groupPosition, childPosition, id);
				return true;
			}
		});
		
		registerForContextMenu(mContactsList);
		return rootView;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Log.d(CLASS, "Resuming contacts fragment");
	}

	@Override
	public void onPause() {
		Log.d(CLASS, "Pausing contacts fragment");

		if (mRoster != null) {
			mRoster.removeRosterListener(mRosterEventHandler);
			mRoster = null;
		}

		super.onPause();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
									ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		//TODO Uncomment if you added static entries to an XML layout
		//MenuInflater inflater = getMenuInflater();
		//inflater.inflate(R.menu.roster_context, menu);
		Log.d(CLASS, "creating context menu");
		Set<String> rooms = ((RosterActivity) getActivity()).getXMPPService().getRooms();
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
		IXMPPService xmpp = ((RosterActivity) getActivity()).getXMPPService(); 
		AsyncTask<String, Void, String[]> inviteTask = new InviteTask(xmpp,
																	  new InviteMateHandler());
		inviteTask.execute(contact, teamName);
	}
	
	private String getExpandableListChild(long packedPosition) {
		final int group_position = ExpandableListView.getPackedPositionGroup(packedPosition);
		final int child_position = ExpandableListView.getPackedPositionChild(packedPosition);
		final Map<String, String> child = (Map<String, String>) mAdapter.getChild(group_position,
																		   child_position);
		return child.get(NAME);
	}
	

	public void handleDisconnect() {
		mExpandableGroups.clear();
		mExpandableChildren.clear();
		mContactsList.post(new Runnable() {
			@Override
			public void run() {
				mAdapter.notifyDataSetChanged();
			}
		});
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

	protected class FetchRosterHandler extends BaseAsyncTaskCallback<Roster> {
		@Override
		public void onTaskCompleted(Roster roster) {
			mRoster = roster;
			mRoster.addRosterListener(mRosterEventHandler);
			mContactsList.post(new Runnable() {
				@Override
				public void run() {
					fillExpandableList(mRoster);
					mAdapter.notifyDataSetChanged();
				}
			});
		}

		@Override
		public void onTaskAborted(Exception e) {
			final String problem = String.format("Could not fetch roster: %s", e.getMessage());
			Toast.makeText(getActivity(), problem, Toast.LENGTH_LONG).show();
		}
	}

	private class InviteMateHandler extends BaseAsyncTaskCallback<String[]> {
		@Override
		public void onTaskCompleted(String[] connection_data) {
			String user_feedback = String.format("You invited %s to %s", connection_data[0], connection_data[1]);
			Toast.makeText(getActivity(), user_feedback, Toast.LENGTH_LONG).show();
		}
	
		@Override
		public void onTaskAborted(Exception e) {
			String problem = String.format("Failed to invite contact to team: %s", e.getMessage());
			Toast.makeText(getActivity(), problem, Toast.LENGTH_LONG).show();
		}
	}

	private class RosterEventHandler implements RosterListener {
	
		@Override
		public void entriesAdded(Collection<String> arg0) {
			Log.d(CLASS, "Entries have been added to the roster");
			redrawOnUiThread();
		}
	
		@Override
		public void entriesDeleted(Collection<String> arg0) {
			Log.d(CLASS, "Entries have been deleted from the roster");
			redrawOnUiThread();
		}
	
		@Override
		public void entriesUpdated(Collection<String> arg0) {
			Log.d(CLASS, "Entries have been updated in the roster");
			redrawOnUiThread();
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
			
			redrawOnUiThread();
		}
		
		private void redrawOnUiThread() {
			mContactsList.post(new Runnable() {
				
				@Override
				public void run() {
					fillExpandableList(mRoster);
					mAdapter.notifyDataSetChanged();	
				}
			});
		}
	}
}