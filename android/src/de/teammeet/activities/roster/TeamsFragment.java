package de.teammeet.activities.roster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.SimpleExpandableListAdapter;
import de.teammeet.R;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.tasks.BaseAsyncTaskCallback;
import de.teammeet.tasks.FetchRoomsTask;

public class TeamsFragment extends Fragment {

	private static final String CLASS = TeamsFragment.class.getSimpleName();
	
	private static final String NAME = "name";
	private static final String AVAILABILITY = "avail";
	
	private SimpleExpandableListAdapter mAdapter;
	private List<Map<String, String>> mExpandableGroups = new ArrayList<Map<String, String>>();
	private List<List<Map<String, String>>> mExpandableChildren = new ArrayList<List<Map<String, String>>>();

	private ExpandableListView mTeamsList;
	
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
				android.R.layout.simple_expandable_list_item_2,
				new String[] { NAME, AVAILABILITY },
				new int[] { android.R.id.text1, android.R.id.text2}
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
		IXMPPService service = ((RosterActivity) getActivity()).getXMPPService();
		if (service != null && service.isAuthenticated()) {
			// service has been connected before this fragment and its view were created
			new FetchRoomsTask(service, new FetchRoomsHandler()).execute();
		}
	}

	@Override
	public void onPause() {
		Log.d(CLASS, "Pausing teams fragement");
		super.onPause();
	}

	private void fillExpandableList(Set<String> rooms) {
		mExpandableGroups.clear();
		mExpandableChildren.clear();

		for (String room : rooms) {
			Map<String, String> roomStruct = new HashMap<String, String>();
			roomStruct.put(NAME, room);
			mExpandableGroups.add(roomStruct);
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
			if (mTeamsList != null) {
				// the service as well as its view have been created
				mTeamsList.post(new Runnable() {
					@Override
					public void run() {
						fillExpandableList(mRooms);
						mAdapter.notifyDataSetChanged();
					}
				});
				}
		}
	}
}
