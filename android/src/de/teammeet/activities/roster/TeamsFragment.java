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
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;
import de.teammeet.R;
import de.teammeet.helper.BroadcastHelper;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.tasks.BaseAsyncTaskCallback;
import de.teammeet.tasks.FetchTeamsTask;

public class TeamsFragment extends Fragment {

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
