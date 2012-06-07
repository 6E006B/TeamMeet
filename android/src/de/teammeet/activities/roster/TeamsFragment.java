package de.teammeet.activities.roster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

import de.teammeet.R;
import de.teammeet.activities.chat.Chat;
import de.teammeet.activities.chat.ChatsActivity;
import de.teammeet.activities.teams.TeamMeetActivity;
import de.teammeet.helper.BroadcastHelper;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.services.xmpp.XMPPService;
import de.teammeet.tasks.BaseAsyncTaskCallback;
import de.teammeet.tasks.FetchTeamsTask;
import de.teammeet.tasks.LeaveTeamTask;

public class TeamsFragment extends SherlockFragment {

	private static final String CLASS = TeamsFragment.class.getSimpleName();
	
	private static final String NAME = "name";
	
	private BroadcastReceiver mConnectReceiver;
	private BroadcastReceiver mDisconnectReceiver;
	private BroadcastReceiver mTeamsUpdateReceiver;
	private ExpandableListView mTeamsList;
	private SimpleExpandableListAdapter mAdapter;
	private List<Map<String, String>> mExpandableGroups = new ArrayList<Map<String, String>>();
	private List<List<Map<String, String>>> mExpandableChildren = new ArrayList<List<Map<String, String>>>();


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(CLASS, String.format("Creating teams fragment '%s'", this));
		
		mAdapter = new SimpleExpandableListAdapter(
				getActivity(),
				mExpandableGroups,
				android.R.layout.simple_expandable_list_item_1,
				new String[] { NAME },
				new int[] { android.R.id.text1},
				mExpandableChildren,
				android.R.layout.simple_expandable_list_item_1,
				new String[] { NAME },
				new int[] { android.R.id.text1 }
				);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(CLASS, "Creating view");
		LinearLayout rootView = (LinearLayout) inflater.inflate(R.layout.teams, container, false);
		mTeamsList = (ExpandableListView) rootView.findViewById(R.id.teams_list);
		mTeamsList.setAdapter(mAdapter);
		mTeamsList.setEmptyView(rootView.findViewById(R.id.teams_empty));
		Log.d(CLASS, String.format("teams list is '%s'", mTeamsList));
		registerForContextMenu(mTeamsList);
		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(CLASS, "Resuming teams fragment");

		mConnectReceiver = BroadcastHelper.getBroadcastReceiverInstance(this,
																		ConnectReceiver.class,
																		R.string.broadcast_connection_state,
																		R.string.broadcast_connected);
		mDisconnectReceiver = BroadcastHelper.getBroadcastReceiverInstance(this,
																		   DisconnectReceiver.class,
																		   R.string.broadcast_connection_state,
																		   R.string.broadcast_disconnected);
		mTeamsUpdateReceiver = BroadcastHelper.getBroadcastReceiverInstance(this,
																			TeamUpdateReceiver.class,
																			R.string.broadcast_teams_updated);
	}

	@Override
	public void onPause() {
		Log.d(CLASS, "Pausing teams fragement");

		Activity activity = getActivity();
		activity.unregisterReceiver(mConnectReceiver);
		activity.unregisterReceiver(mDisconnectReceiver);
		activity.unregisterReceiver(mTeamsUpdateReceiver);

		super.onPause();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		Log.d(CLASS, "TeamsFragment.onCreateContextMenu()");
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.team_context, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Log.d(CLASS, "TeamsFragment.onContextItemSelected()");
		switch(item.getItemId()) {
		case R.id.teams_list_context_open_map:
			clickedOpenMap(item);
			return true;
		case R.id.teams_list_context_open_chat:
			clickedOpenChat(item);
			return true;
		case R.id.teams_list_context_leave:
			clickedLeaveTeam(item);
			return true;
		default:
			Log.d(CLASS, String.format("unhandeled item clicked: 0x%x", item.getItemId()));
			return super.onContextItemSelected(item);
		}
	}

	private void fillExpandableList(Set<String> teams) {
		mExpandableGroups.clear();
		mExpandableChildren.clear();
		
		IXMPPService xmppService = ((RosterActivity) getActivity()).getXMPPService();

		for (String teamName : teams) {
			Map<String, String> teamStruct = new HashMap<String, String>();
			List<Map<String, String>> matesStruct = new ArrayList<Map<String,String>>();
			String me = "";
			
			teamStruct.put(NAME, teamName);
			mExpandableGroups.add(teamStruct);
			
			try {
				me = xmppService.getNickname(teamName);
			} catch (XMPPException e) {
				String problem = String.format("Failed to fetch own nickname in team '%s': %s",
												teamName, e.getMessage());
				Log.e(CLASS, problem, e);
				Toast.makeText(getActivity(), problem, Toast.LENGTH_LONG).show();
			}

			try {
				Iterator<String> mates = xmppService.getMates(teamName);
				while (mates.hasNext()) {
					String nick = StringUtils.parseResource(mates.next());
					if (!nick.equals(me)) {
						HashMap<String, String> mateStruct = new HashMap<String, String>();
						mateStruct.put(NAME, nick);
						matesStruct.add(mateStruct);
					}
				}
				mExpandableChildren.add(matesStruct);
			} catch (XMPPException e) {
				String problem = String.format("Failed to fetch mates in team '%s': %s", teamName, e.getMessage());
				Log.e(CLASS, problem, e);
				Toast.makeText(getActivity(), problem, Toast.LENGTH_LONG).show();
			}
		}

	}

	public void handleDisconnect() {
		mExpandableGroups.clear();
		mExpandableChildren.clear();
		mTeamsList.post(new Runnable() {
			@Override
			public void run() {
				mAdapter.notifyDataSetChanged();
			}
		});
	}

	private void clickedOpenMap(MenuItem item) {
		ExpandableListContextMenuInfo menuInfo = (ExpandableListContextMenuInfo)item.getMenuInfo();
		String team = getExpandableListChild(menuInfo.packedPosition);
		Intent intent = new Intent(getActivity().getApplicationContext(), TeamMeetActivity.class);
		intent.putExtra(XMPPService.GROUP, team);
		startActivity(intent);
	}

	private void clickedOpenChat(MenuItem item) {
		ExpandableListContextMenuInfo menuInfo = (ExpandableListContextMenuInfo)item.getMenuInfo();
		String team = getExpandableListChild(menuInfo.packedPosition);
		Intent intent = new Intent(getActivity().getApplicationContext(), ChatsActivity.class);
		intent.putExtra(XMPPService.TYPE, Chat.TYPE_GROUP_CHAT);
		intent.putExtra(XMPPService.SENDER, team);
		startActivity(intent);
	}

	private void clickedLeaveTeam(MenuItem item) {
		ExpandableListContextMenuInfo menuInfo = (ExpandableListContextMenuInfo)item.getMenuInfo();
		String team = getExpandableListChild(menuInfo.packedPosition);
		Toast.makeText(getActivity(), String.format("Want to leave %s", team), Toast.LENGTH_SHORT).
			  show();
		new LeaveTeamTask(((RosterActivity)getActivity()).getXMPPService(),
		                  new LeaveTeamHandler()).execute(team);
	}

	private String getExpandableListChild(long packedPosition) {
		final int group_position = ExpandableListView.getPackedPositionGroup(packedPosition);
		final Map<String, String> group = (Map<String, String>) mAdapter.getGroup(group_position);
		return group.get(NAME);
	}

	protected class FetchTeamsHandler extends BaseAsyncTaskCallback<Set<String>> {
		private Set<String> mTeams;

		@Override
		public void onTaskCompleted(Set<String> teams) {
			mTeams = teams;
			mTeamsList.post(new Runnable() {
				@Override
				public void run() {
					fillExpandableList(mTeams);
					mAdapter.notifyDataSetChanged();
				}
			});
		}

		@Override
		public void onTaskAborted(Exception e) {
			final String problem = String.format("Could not fetch teams: %s", e.getMessage());
			Toast.makeText(getActivity(), problem, Toast.LENGTH_LONG).show();
		}
	}

	protected class LeaveTeamHandler extends BaseAsyncTaskCallback<String> {
		@Override
		public void onTaskCompleted(String team) {
			final String message = String.format("Left team %s", team);
			Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
			Intent teamsChangedIntent = new Intent(getString(R.string.broadcast_teams_updated));
			getActivity().sendBroadcast(teamsChangedIntent);
		}

		@Override
		public void onTaskAborted(Exception e) {
			final String problem = String.format("Could leave team: %s", e.getMessage());
			Toast.makeText(getActivity(), problem, Toast.LENGTH_LONG).show();
		}
	}

	private class ConnectReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(CLASS, String.format("*** Received CONNECT broadcast in '%s'", TeamsFragment.this));
			
			IXMPPService service = ((RosterActivity) getActivity()).getXMPPService();
			new FetchTeamsTask(service, new FetchTeamsHandler()).execute();
		}
	}

	private class DisconnectReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(CLASS, String.format("*** Received DISCONNECT broadcast in '%s'", TeamsFragment.this));
			handleDisconnect();
		}
	}

	private class TeamUpdateReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(CLASS, String.format("*** Received TEAMS_UPDATED broadcast in '%s'", TeamsFragment.this));

			IXMPPService service = ((RosterActivity) getActivity()).getXMPPService();
			new FetchTeamsTask(service, new FetchTeamsHandler()).execute();
		}
	}
}
