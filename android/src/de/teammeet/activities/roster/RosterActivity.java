package de.teammeet.activities.roster;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.teammeet.R;
import de.teammeet.activities.preferences.SettingsActivity;
import de.teammeet.activities.teams.TeamMeetActivity;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.services.xmpp.XMPPService;
import de.teammeet.tasks.BaseAsyncTaskCallback;
import de.teammeet.tasks.ConnectTask;
import de.teammeet.tasks.CreateGroupTask;
import de.teammeet.tasks.DisconnectTask;

public class RosterActivity extends SherlockFragmentActivity {
	private static String CLASS = RosterActivity.class.getSimpleName();
	private static String CONTACTS_TAB_ID = "contacts_tab";
	private static String TEAMS_TAB_ID = "teams_tab";
	private static String SAVED_TAB_KEY = "last_tab";

	private IXMPPService mXMPPService = null;
	private XMPPServiceConnection mXMPPServiceConnection = new XMPPServiceConnection();
	private Intent mCurrentIntent = null;
	
	private ViewPager mViewPager;
	private RosterAdapter mPagerAdapter;
	
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(CLASS, "Creating tabbed roster activity");
		
		// Inflate the layout
		setContentView(R.layout.tabbed_roster);
		
		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(false);
		
		Tab tab = actionBar.newTab();
		tab.setText(R.string.tab_contacts);
		tab.setTabListener(new TabListener<ContactsFragment>(
						this, CONTACTS_TAB_ID, ContactsFragment.class));
		actionBar.addTab(tab);

		tab = actionBar.newTab();
		tab.setText(R.string.tab_teams);
		tab.setTabListener(new TabListener<TeamsFragment>(
					this, TEAMS_TAB_ID, TeamsFragment.class));
		actionBar.addTab(tab);
		
		if (savedInstanceState != null) {
			//set the tab as per the saved state
			actionBar.setSelectedNavigationItem(savedInstanceState.getInt(SAVED_TAB_KEY));
		}

		// Intialise ViewPager
		//intialiseViewPager();

		mCurrentIntent = getIntent();
	}

	@Override
	protected void onResume() {
		Log.d(CLASS, "Resuming tabbed roster activity");
		// create the service (if it isn't already running)
		final Intent xmppIntent = new Intent(this, XMPPService.class);
		startService(xmppIntent);

		Log.d(CLASS, "started XMPP service");

		// now connect to the service
		boolean bindSuccess = bindService(xmppIntent, mXMPPServiceConnection, 0);
		if (bindSuccess) {
			Log.d(CLASS, "onResume(): bind to XMPP service succeeded");
		} else {
			Log.e(CLASS, "onResume(): bind to XMPP service failed");
			Toast.makeText(this, "Couldn't connect to XMPP service.", Toast.LENGTH_LONG).show();
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		Log.d(CLASS, "Pausing tabbed roster activity");

		if (mXMPPServiceConnection != null) {
			unbindService(mXMPPServiceConnection);
		}
		mXMPPService = null;

		super.onPause();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		mCurrentIntent = intent;
	}

	/** (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onSaveInstanceState(android.os.Bundle)
	 */
	protected void onSaveInstanceState(Bundle outState) {
		ActionBar bar = getSupportActionBar();
		outState.putInt(SAVED_TAB_KEY, bar.getSelectedNavigationIndex()); //save the tab selected
		super.onSaveInstanceState(outState);
	}

	/*
	 * Quick fix to make XMPP Service available to fragments.
	 */
	public IXMPPService getXMPPService() {
		return mXMPPService;
	}

	/**
	 * Initialise ViewPager
	 */
	private void intialiseViewPager() {

		mPagerAdapter  = new RosterAdapter(getSupportFragmentManager());

		mViewPager = (ViewPager) findViewById(R.id.viewpager);
		mViewPager.setAdapter(mPagerAdapter);
		//mViewPager.setOnPageChangeListener(this);

	}

	private void handleIntent(Intent intent) {
		Log.d(CLASS, "handling intent");
		Bundle extras = intent.getExtras();
		if (extras != null) {
			Log.d(CLASS, "extras: " + extras.toString());
		} else {
			Log.d(CLASS, "no extras");
		}
		final int type = intent.getIntExtra(XMPPService.TYPE, XMPPService.TYPE_NONE);
		intent.removeExtra(XMPPService.TYPE);
		switch (type) {
		case XMPPService.TYPE_JOIN:
			Log.d(CLASS, "Intent to join a group");
			handleJoinIntent(intent);
			break;
		default:
			Log.d(CLASS, "Intent of unknown type: " + type);
			break;
		}
	}

	private void handleJoinIntent(Intent intent) {
		final String room = intent.getStringExtra(XMPPService.ROOM);
		final String inviter = intent.getStringExtra(XMPPService.INVITER);
		final String reason = intent.getStringExtra(XMPPService.REASON);
		final String password = intent.getStringExtra(XMPPService.PASSWORD);
		final String from = intent.getStringExtra(XMPPService.FROM);

		Log.d(CLASS, String.format("room: '%s' inviter: '%s' reason: '%s' password: '%s' from: '%s'",
									room, inviter, reason, password, from));

		// cleanup the extras so that this is only executed once, not every time the activity is
		// brought to foreground again
		cleanupJoinIntent(intent);

		if (room != null && inviter != null && reason != null && from != null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Group Invitation");
			builder.setMessage(String.format("%s wants you to join '%s':\n%s",
											 StringUtils.parseName(inviter),
											 StringUtils.parseName(room),
											 reason)
											);
			builder.setCancelable(false);
			builder.setPositiveButton("Join", new DialogInterface.OnClickListener() {
					   public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
							final SharedPreferences settings = PreferenceManager.
									getDefaultSharedPreferences(RosterActivity.this);
							final String userIDKey = getString(R.string.preference_user_id_key);
							final String userID = settings.getString(userIDKey, "anonymous");
							try {
								mXMPPService.joinRoom(room, userID, password);
							} catch (XMPPException e) {
								String problem = String.format("Unable to join room '%s': %s",
																room, e.getMessage());
								Log.e(CLASS, problem, e);
								Toast.makeText(RosterActivity.this, problem, Toast.LENGTH_LONG).show();
							}
					   }
				   });
			builder.setNegativeButton("Decline", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
			final AlertDialog alert = builder.create();
			alert.show();
		} else {
			Log.e(CLASS, "Cannot handle invite: Missing parameters.");
		}
	}

	private void cleanupJoinIntent(Intent intent) {
		intent.removeExtra(XMPPService.ROOM);
		intent.removeExtra(XMPPService.INVITER);
		intent.removeExtra(XMPPService.REASON);
		intent.removeExtra(XMPPService.PASSWORD);
		intent.removeExtra(XMPPService.FROM);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.roster, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem connectMenu = menu.findItem(R.id.roster_menu_connect);
		MenuItem formTeamMenu = menu.findItem(R.id.roster_menu_form_team);

		Resources res = getResources();
		int connectTitle = R.string.roster_menu_connect;
		CharSequence connectTitleCondensed = res.getString(R.string.roster_menu_connect_condensed);
		boolean enableConnect = false;
		boolean showFormTeam = false;

		if (mXMPPService != null) {
			enableConnect = true;

			if (mXMPPService.isAuthenticated()) {
				Log.d(CLASS, "setting menu option to 'disconnect'");
				connectTitle = R.string.roster_menu_disconnect;
				connectTitleCondensed = res.getString(R.string.roster_menu_disconnect_condensed);
				showFormTeam = true;
			}
		}
		connectMenu.setTitle(connectTitle);
		connectMenu.setTitleCondensed(connectTitleCondensed);
		connectMenu.setEnabled(enableConnect);
		formTeamMenu.setVisible(showFormTeam);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {

		switch (item.getItemId()) {
			case R.id.roster_menu_connect:
				Log.d(CLASS, "User clicked 'connect' in menu");
				clickedConnect(item);
				return true;

			case R.id.roster_menu_show_map:
				Log.d(CLASS, "User clicked 'map' in menu");
				clickedMap();
				return true;

			case R.id.roster_menu_form_team:
				Log.d(CLASS, "User clicked 'form team' in menu");
				clickedFormTeam();
				return true;

			case R.id.roster_menu_settings:
				Log.d(CLASS, "User clicked 'form team' in menu");
				clickedSettings();
				return true;

			case R.id.roster_menu_exit:
				Log.d(CLASS, "User clicked 'exit' in menu");
				clickedExit();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void clickedSettings() {
		final Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	private void clickedExit() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				mXMPPService.disconnect();
			}
		}).start();
		final Intent intent = new Intent(this, XMPPService.class);
		stopService(intent);
		finish();
	}

	private void clickedMap() {
		final Intent intent = new Intent(this, TeamMeetActivity.class);
		startActivity(intent);
	}

	private void clickedConnect(MenuItem connectMenu) {
		connectMenu.setEnabled(false);
		if (mXMPPService.isAuthenticated()) {
			new DisconnectTask((XMPPService)mXMPPService, new DisconnectHandler()).execute();
		} else {
			new ConnectTask((XMPPService)mXMPPService, new ConnectHandler()).execute();
		}
	}

	private void clickedFormTeam() {
		Log.d(CLASS, "Will display 'formTeamDialog' now");
		FormTeamDialog dialog = new FormTeamDialog();
		FragmentManager fm = getSupportFragmentManager();
		//TODO check how to spawn dialog fragment from activity
		dialog.show(fm, null);
	}

	public void enteredTeamName(String teamName) {
		Log.d(CLASS, String.format("Will create team '%s'", teamName));
		final SharedPreferences settings =
				PreferenceManager.getDefaultSharedPreferences(this);
		final String conferenceSrvKey = getString(R.string.preference_conference_server_key);
		final String conferenceSrv = settings.getString(conferenceSrvKey, "");
		new CreateGroupTask(mXMPPService, new FormTeamHandler()).execute(teamName, conferenceSrv);
	}

	private void toggleConnectionStateBroadcast(int oldStateAction, int newStateAction) {
		Log.d(CLASS, "Cutting off old connection broadcast");
		Intent intent = new Intent();
		intent.addCategory(getString(R.string.broadcast_connection_state));
		intent.setAction(getString(oldStateAction));
		removeStickyBroadcast(intent);

		Log.d(CLASS, "Turning on new connection broadcast");
		intent = new Intent();
		intent.addCategory(getString(R.string.broadcast_connection_state));
		intent.setAction(getString(newStateAction));
		sendStickyBroadcast(intent);
	}

	private class XMPPServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(CLASS, "RosterActivity has been (re-)bound to XMPP service ('" + className + "')");
			mXMPPService = ((XMPPService.LocalBinder) binder).getService();
			handleIntent(mCurrentIntent);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			Log.d(CLASS, "RosterActivity.XMPPServiceConnection.onServiceDisconnected('" + className + "')");
			mXMPPService = null;
		}
	};

	private class ConnectHandler extends BaseAsyncTaskCallback<Void> {
		@Override
		public void onTaskCompleted(Void nothing) {
			Log.d(CLASS, "Connect task completed!!");
			invalidateOptionsMenu();

			// broadcast connected
			toggleConnectionStateBroadcast(R.string.broadcast_disconnected,
										   R.string.broadcast_connected);
		}

		@Override
		public void onTaskAborted(Exception e) {
			String problem = String.format("Failed to connect to XMPP server: %s", e.getMessage());
			Toast.makeText(RosterActivity.this, problem, Toast.LENGTH_LONG).show();
		}
	}

	private class DisconnectHandler extends BaseAsyncTaskCallback<Void> {
		@Override
		public void onTaskCompleted(Void result) {
			Log.d(CLASS, "you're now disconnected");
			invalidateOptionsMenu();

			// broadcast disconnected
			toggleConnectionStateBroadcast(R.string.broadcast_connected,
										   R.string.broadcast_disconnected);
		}
	}

	private class FormTeamHandler extends BaseAsyncTaskCallback<String[]> {
		@Override
		public void onTaskCompleted(String[] connection_data) {
			String user_feedback = String.format("Founded team '%s'", connection_data[0]);
			Toast.makeText(RosterActivity.this, user_feedback, Toast.LENGTH_LONG).show();

			Intent newTeam = new Intent(getString(R.string.broadcast_teams_updated));
			sendBroadcast(newTeam);
		}

		@Override
		public void onTaskAborted(Exception e) {
			String problem = String.format("Failed to form team: %s", e.getMessage());
			Toast.makeText(RosterActivity.this, problem, Toast.LENGTH_LONG).show();
		}
	}
}
