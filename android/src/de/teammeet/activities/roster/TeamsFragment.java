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
import android.content.IntentFilter;
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
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.tasks.BaseAsyncTaskCallback;
import de.teammeet.tasks.FetchRoomsTask;

public class TeamsFragment extends Fragment {

	private static final String CLASS = TeamsFragment.class.getSimpleName();
	
	private static final String NAME = "name";
	private static final String AFFILIATION = "affiliation";
	
	private BroadcastReceiver mConnectReceiver;
	private BroadcastReceiver mDisconnectReceiver;
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

		mConnectReceiver = new ConnectReceiver();
		IntentFilter connectFilter = new IntentFilter(getActivity().getString(R.string.broadcast_connected));
		getActivity().registerReceiver(mConnectReceiver, connectFilter);

		mDisconnectReceiver = new DisconnectReceiver();
		IntentFilter disconnectFilter = new IntentFilter(getActivity().getString(R.string.broadcast_disconnected));
		getActivity().registerReceiver(mDisconnectReceiver, disconnectFilter);
	}

	@Override
	public void onPause() {
		Log.d(CLASS, "Pausing teams fragement");

		Activity activity = getActivity();
		activity.unregisterReceiver(mConnectReceiver);
		activity.unregisterReceiver(mDisconnectReceiver);

		super.onPause();
	}

	private void fillExpandableList(Set<String> rooms) {
		mExpandableGroups.clear();
		mExpandableChildren.clear();
		
		IXMPPService xmppService = ((RosterActivity) getActivity()).getXMPPService();

		for (String room : rooms) {
			Map<String, String> roomStruct = new HashMap<String, String>();
			List<Map<String, String>> membersStruct = new ArrayList<Map<String,String>>();
			String me = "";
			
			roomStruct.put(NAME, room);
			mExpandableGroups.add(roomStruct);
			
			try {
				me = xmppService.getNickname(room);
			} catch (XMPPException e) {
				String problem = String.format("Failed to fetch own nickname from room '%s': %s",
												room, e.getMessage());
				Log.e(CLASS, problem, e);
				Toast.makeText(getActivity(), problem, Toast.LENGTH_LONG).show();
			}

			try {
				Iterator<String> occupants = xmppService.getOccupants(room);
				while (occupants.hasNext()) {
					String nick = StringUtils.parseResource(occupants.next());
					if (!nick.equals(me)) {
						HashMap<String, String> occupantStruct = new HashMap<String, String>();
						occupantStruct.put(NAME, nick);
						membersStruct.add(occupantStruct);
					}
				}
				mExpandableChildren.add(membersStruct);
			} catch (XMPPException e) {
				String problem = String.format("Failed to fetch occupants for room '%s': %s", room, e.getMessage());
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

	protected class FetchRoomsHandler extends BaseAsyncTaskCallback<Set<String>> {
		private Set<String> mRooms;

		@Override
		public void onTaskCompleted(Set<String> rooms) {
			mRooms = rooms;
			mTeamsList.post(new Runnable() {
				@Override
				public void run() {
					fillExpandableList(mRooms);
					mAdapter.notifyDataSetChanged();
				}
			});
		}
	}

	public class ConnectReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(CLASS, String.format("*** Received CONNECT broadcast in '%s'", TeamsFragment.this));
			
			IXMPPService service = ((RosterActivity) getActivity()).getXMPPService();
			new FetchRoomsTask(service, new FetchRoomsHandler()).execute();
		}
	}

	public class DisconnectReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(CLASS, String.format("*** Received DISCONNECT broadcast in '%s'", TeamsFragment.this));
			handleDisconnect();
		}
	}
}
