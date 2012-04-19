package de.teammeet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.XMPPException;

import android.app.ExpandableListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ExpandableListAdapter;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.xmpp.XMPPService;


/**
 * Demonstrates expandable lists backed by a Simple Map-based adapter
 */
public class RosterActivity extends ExpandableListActivity {
	private static final String CLASS = RosterActivity.class.getSimpleName();
	private static final String NAME = "name";

	private Map<String, List<String>> mContacts = null;
	private ExpandableListAdapter mAdapter = null;

	private IXMPPService mXMPPService = null;
	private XMPPServiceConnection mXMPPServiceConnection = new XMPPServiceConnection();

	private class XMPPServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(CLASS, "RosterActivity.XMPPServiceConnection.onServiceConnected('" + className + "')");
			mXMPPService = ((XMPPService.LocalBinder) binder).getService();
			
			if (mContacts == null) {
				try {
					mContacts = mXMPPService.getContacts();
				} catch (XMPPException e) {
					e.printStackTrace();
					String problem = String.format("Could not fetch contacts: %s", e.getMessage());
					Log.e(CLASS, problem);
					Toast.makeText(getApplicationContext(), problem, 5);
				}
				
				fillExpandableList(mContacts);
			}

		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			Log.d(CLASS, "RosterActivity.XMPPServiceConnection.onServiceDisconnected('" + className + "')");
			mXMPPService = null;
		}
	};


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
	
	private void fillExpandableList(Map<String, List<String>> contacts) {

		List<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
		List<List<Map<String, String>>> childData = new ArrayList<List<Map<String, String>>>();
	
		for (String groupName : contacts.keySet()) {
			Map<String, String> curGroupMap = new HashMap<String, String>();
			List<String> groupContacts = contacts.get(groupName);
			groupData.add(curGroupMap);
			curGroupMap.put(NAME, groupName);
	
	
			List<Map<String, String>> children = new ArrayList<Map<String, String>>();
			for (String contact : groupContacts) {
				Map<String, String> curChildMap = new HashMap<String, String>();
				children.add(curChildMap);
				curChildMap.put(NAME, contact);
			}
			childData.add(children);
		}
	
		// Set up our adapter
		mAdapter = new SimpleExpandableListAdapter(
				this,
				groupData,
				android.R.layout.simple_expandable_list_item_1,
				new String[] { NAME },
				new int[] { android.R.id.text1},
				childData,
				android.R.layout.simple_expandable_list_item_1,
				new String[] { NAME },
				new int[] { android.R.id.text1}
				);
		setListAdapter(mAdapter);
	}
	

}