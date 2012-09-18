package de.teammeet.activities.roster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwoLineListItem;
import de.teammeet.R;
import de.teammeet.activities.chat.Chat;
import de.teammeet.activities.chat.ChatsActivity;
import de.teammeet.helper.BroadcastHelper;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.services.xmpp.XMPPService;
import de.teammeet.tasks.BaseAsyncTaskCallback;
import de.teammeet.tasks.FetchRosterTask;
import de.teammeet.tasks.InviteTask;
import de.teammeet.tasks.RemoveMateTask;


/**
 * Demonstrates expandable lists backed by a Simple Map-based adapter
 */
public class ContactsFragment extends Fragment {
	private static final String CLASS = ContactsFragment.class.getSimpleName();
	private static final String UNFILED_GROUP = "Unfiled contacts";
	private static final int CONTEXT_MENU_INVITE_PARENT_ID = 0x7e000002;
	private static final int CONTEXT_MENU_INVITE_ROOM_ID = 0x7e000003;
	private static final int CONTEXT_MENU_REMOVE_MATE_ID = 0x7e000004;
	private static final int CONTEXT_MENU_OPEN_CHAT_ID = 0x7e000005;

	private BroadcastReceiver mConnectReceiver;
	private BroadcastReceiver mDisconnectReceiver;
	private Roster mRoster = null;
	private RosterListener mRosterEventHandler;
	private ExpandableListView mContactsList;
	private ContactlistAdapter mAdapter;
	private List<String> mExpandableGroups = new ArrayList<String>();
	private List<List<ContactlistChild>> mExpandableChildren = new ArrayList<List<ContactlistChild>>();
	private ExpandableListContextMenuInfo mLastContextItemInfo;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS, String.format("Creating contacts fragment"));
		
		mAdapter = new ContactlistAdapter(
				getActivity(),
				mExpandableGroups,
				mExpandableChildren
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
				openChat(contact);
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

		mConnectReceiver = BroadcastHelper.getBroadcastReceiverInstance(this,
																		ConnectReceiver.class,
																		R.string.broadcast_connection_state,
																		R.string.broadcast_connected);
		mDisconnectReceiver = BroadcastHelper.getBroadcastReceiverInstance(this,
																		   DisconnectReceiver.class,
																		   R.string.broadcast_connection_state,
																		   R.string.broadcast_disconnected);
	}

	@Override
	public void onPause() {
		Log.d(CLASS, "Pausing contacts fragment");

		if (mRoster != null) {
			mRoster.removeRosterListener(mRosterEventHandler);
			mRoster = null;
		}

		Activity activity = getActivity();
		activity.unregisterReceiver(mConnectReceiver);
		activity.unregisterReceiver(mDisconnectReceiver);

		super.onPause();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
									ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		ExpandableListView.ExpandableListContextMenuInfo info =
				(ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
		final int type = ExpandableListView.getPackedPositionType(info.packedPosition);
//		final int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
//		final int child = ExpandableListView.getPackedPositionChild(info.packedPosition);

		//Only create a context menu for child items
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			createChildContextMenu(menu);
		} else {
			// long-press on a group
			Log.d(CLASS, "Long-Press on a group.");
			createGroupContextMenu(menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Log.d(CLASS, String.format("Context item '%s' clicked", item.getTitleCondensed()));

		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			switch(item.getItemId()) {
				case CONTEXT_MENU_OPEN_CHAT_ID:
					clickedOpenChat(item);
					return true;
				case CONTEXT_MENU_INVITE_PARENT_ID:
					/* backup info, will need it when sub-menu item gets selected.
					 * cf http://code.google.com/p/android/issues/detail?id=7139.
					 */
					mLastContextItemInfo = ((ExpandableListContextMenuInfo)item.getMenuInfo());
					return true;
				case CONTEXT_MENU_INVITE_ROOM_ID:
					clickedInviteToTeam(item);
					return true;
				case CONTEXT_MENU_REMOVE_MATE_ID:
					clickedRemoveMate(item);
					return true;
				default:
					Log.d(CLASS, String.format("unhandeled context menu item clicked: 0x%x",
					                           item.getItemId()));
			}
		} else {
			// selected item is a group
			Log.d(CLASS, "Clicked on a context menu item regarding a group.");
			switch(item.getItemId()) {
			case R.id.roster_list_context_group_add_contact:
				clickedAddContactToGroup(item);
				return true;
			default:
				Log.d(CLASS, String.format("unhandeled context menu item clicked: 0x%x",
				                           item.getItemId()));
			}
		}
		return super.onContextItemSelected(item);
	}

	private void createChildContextMenu(ContextMenu menu) {
		//TODO Uncomment if you added static entries to an XML layout
		//MenuInflater inflater = getMenuInflater();
		//inflater.inflate(R.menu.roster_context, menu);
		Log.d(CLASS, "creating context menu");

		menu.add(Menu.NONE, CONTEXT_MENU_OPEN_CHAT_ID, Menu.NONE, R.string.context_open_chat);
		try {
			Set<String> teams = ((RosterActivity) getActivity()).getXMPPService().getTeams();
			if (!teams.isEmpty()) {
				SubMenu inviteSubMenu = menu.addSubMenu(Menu.NONE, CONTEXT_MENU_INVITE_PARENT_ID,
														Menu.NONE, R.string.context_invite);
				for (String teamName : teams) {
					Log.d(CLASS, "team: " + teamName);
					inviteSubMenu.add(Menu.NONE, CONTEXT_MENU_INVITE_ROOM_ID, Menu.NONE, teamName);
				}
			}
		} catch (XMPPException e) {
			String problem = String.format("Could not fetch teams: %s", e.getMessage());
			Log.e(CLASS, problem);
			Toast.makeText(getActivity(), problem, Toast.LENGTH_LONG).show();
		}
		menu.add(Menu.NONE, CONTEXT_MENU_REMOVE_MATE_ID, Menu.NONE, R.string.context_remove_mate);
	}

	private void createGroupContextMenu(ContextMenu menu) {
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.roster_context_group, menu);
	}

	private void clickedOpenChat(MenuItem item) {
		ExpandableListContextMenuInfo menuInfo = (ExpandableListContextMenuInfo)item.getMenuInfo();
		final String contact = getExpandableListChild(menuInfo.packedPosition);
		openChat(contact);
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

	private void clickedRemoveMate(MenuItem item) {
		ExpandableListContextMenuInfo menuInfo = (ExpandableListContextMenuInfo)item.getMenuInfo();
		String contact = getExpandableListChild(menuInfo.packedPosition);
		Log.d(CLASS, String.format("desire to remove contact '%s'", contact));
		IXMPPService xmpp = ((RosterActivity) getActivity()).getXMPPService();
		AsyncTask<String, Void, String> removeMateTask =
				new RemoveMateTask(xmpp, new RemoveMateHandler());
		removeMateTask.execute(contact);
	}

	private void clickedAddContactToGroup(MenuItem item) {
		ExpandableListContextMenuInfo menuInfo = (ExpandableListContextMenuInfo)item.getMenuInfo();
		final String group = getExpandableListGroup(menuInfo.packedPosition);
		RosterActivity activity = (RosterActivity)getActivity();
		AddContactDialog dialog = new AddContactDialog(activity.getXMPPService(), group);
		dialog.show(getFragmentManager(), null);
	}

	private String getExpandableListChild(long packedPosition) {
		final int group_position = ExpandableListView.getPackedPositionGroup(packedPosition);
		final int child_position = ExpandableListView.getPackedPositionChild(packedPosition);
		final ContactlistChild child = (ContactlistChild) mAdapter.getChild(group_position,
																				  child_position);
		return child.mName;
	}

	private String getExpandableListGroup(long packedPosition) {
		final int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
		String group = (String) mAdapter.getGroup(groupPosition);
		return group;
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

	private void openChat(String contact) {
		Intent intent = new Intent(getActivity(), ChatsActivity.class);
		intent.putExtra(XMPPService.TYPE, Chat.TYPE_NORMAL_CHAT);
		intent.putExtra(XMPPService.SENDER, contact);
		startActivity(intent);
	}

	private class ExpandableContactEntry {
		protected String mGroup;
		protected List<ContactlistChild> mChildren;
	
		public ExpandableContactEntry(String groupName, Collection<RosterEntry> contacts, Roster roster) {
			mGroup = groupName;
			mChildren = new ArrayList<ContactlistChild>();

			for (RosterEntry contact : contacts) {
				String jid = contact.getUser();
				String status = roster.getPresence(jid).toString();
				ContactlistChild newChild = new ContactlistChild(jid, status);
				mChildren.add(newChild);
			}
		}
	}

	protected class ContactlistChild {

		protected String mName = null;
		protected String mStatus = null;

		public ContactlistChild(String name, String status) {
			mName = name;
			mStatus = status;
		}
	}

	private class FetchRosterHandler extends BaseAsyncTaskCallback<Roster> {
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

	private class RemoveMateHandler extends BaseAsyncTaskCallback<String> {
		@Override
		public void onTaskCompleted(String contact) {
			String user_feedback = String.format("You removed %s", contact);
			Toast.makeText(getActivity(), user_feedback, Toast.LENGTH_LONG).show();
		}

		@Override
		public void onTaskAborted(Exception e) {
			String problem = String.format("Failed to remove contact: %s", e.getMessage());
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

	private class ConnectReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(CLASS, String.format("*** Received CONNECT broadcast in '%s'", ContactsFragment.this));
			
			IXMPPService service = ((RosterActivity) getActivity()).getXMPPService();
			new FetchRosterTask(service, new FetchRosterHandler()).execute();
		}
	}

	private class DisconnectReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(CLASS, String.format("*** Received DISCONNECT broadcast in '%s'", ContactsFragment.this));
			handleDisconnect();
		}
	}
}