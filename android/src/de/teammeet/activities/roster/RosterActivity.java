package de.teammeet.activities.roster;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
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
import de.teammeet.activities.chat.TabsAdapter;
import de.teammeet.activities.preferences.SettingsActivity;
import de.teammeet.helper.BroadcastHelper;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.services.xmpp.XMPPService;
import de.teammeet.tasks.AddContactTask;
import de.teammeet.tasks.BaseAsyncTaskCallback;
import de.teammeet.tasks.ConnectTask;
import de.teammeet.tasks.DisconnectTask;
import de.teammeet.tasks.FormTeamTask;
import de.teammeet.tasks.JoinTeamTask;

public class RosterActivity extends SherlockFragmentActivity {
	private static String CLASS = RosterActivity.class.getSimpleName();
	private static String SAVED_TAB_KEY = "last_tab";

	private IXMPPService mXMPPService = null;
	private XMPPServiceConnection mXMPPServiceConnection = new XMPPServiceConnection();
	private Intent mCurrentIntent = null;
	
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(CLASS, "Creating tabbed roster activity");
		
		// Inflate the layout
		setContentView(R.layout.tabbed_roster);
		
		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(false);
		
		// Intialise ViewPager
		intialiseViewPager(actionBar);

		if (savedInstanceState != null) {
			//set the tab as per the saved state
			actionBar.setSelectedNavigationItem(savedInstanceState.getInt(SAVED_TAB_KEY));
		}

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
	private void intialiseViewPager(ActionBar bar) {
		ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
		TabsAdapter tabsAdapter = new TabsAdapter(this, bar, viewPager);

		Tab tab = bar.newTab();
		tab.setText(R.string.tab_contacts);
		tab.setIcon(R.drawable.social_person);
		tabsAdapter.addTab(tab, ContactsFragment.class, null);

		tab = bar.newTab();
		tab.setText(R.string.tab_teams);
		tab.setIcon(R.drawable.social_group);
		tabsAdapter.addTab(tab, TeamsFragment.class, null);
	}

	private void handleIntent(Intent intent) {
		Log.d(CLASS, "handling intent");
		final int type = intent.getIntExtra(XMPPService.TYPE, XMPPService.TYPE_NONE);
		intent.removeExtra(XMPPService.TYPE);
		switch (type) {
		case XMPPService.TYPE_JOIN:
			Log.d(CLASS, "Intent to join a group");
			handleJoinIntent(intent);
			break;
		default:
			Log.d(CLASS, String.format("Intent with generic type '%d'", type));
			break;
		}
	}

	private void handleJoinIntent(Intent intent) {
		final String team = intent.getStringExtra(XMPPService.ROOM);
		final String inviter = StringUtils.parseBareAddress(intent.getStringExtra(XMPPService.INVITER));
		final String reason = intent.getStringExtra(XMPPService.REASON);
		final String password = intent.getStringExtra(XMPPService.PASSWORD);

		Log.d(CLASS, String.format("team: '%s' inviter: '%s' reason: '%s' password: '%s'",
									team, inviter, reason, password));

		// cleanup the extras so that this is only executed once, not every time the activity is
		// brought to foreground again
		cleanupJoinIntent(intent);

		if (team != null && inviter != null && reason != null) {
			displayDialog(new JoinTeamDialog(team, inviter, reason, password));
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
		MenuItem removeMateMenu = menu.findItem(R.id.roster_menu_add_contact);

		Resources res = getResources();
		int connectTitle = R.string.roster_menu_connect;
		CharSequence connectTitleCondensed = res.getString(R.string.roster_menu_connect_condensed);
		boolean enableConnect = false;
		boolean connected = false;

		if (mXMPPService != null) {
			enableConnect = true;

			if (mXMPPService.isAuthenticated()) {
				Log.d(CLASS, "setting menu option to 'disconnect'");
				connectTitle = R.string.roster_menu_disconnect;
				connectTitleCondensed = res.getString(R.string.roster_menu_disconnect_condensed);
				connected = true;
			}
		}
		connectMenu.setTitle(connectTitle);
		connectMenu.setTitleCondensed(connectTitleCondensed);
		connectMenu.setEnabled(enableConnect);
		formTeamMenu.setVisible(connected);
		removeMateMenu.setVisible(connected);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {

		switch (item.getItemId()) {
			case R.id.roster_menu_connect:
				Log.d(CLASS, "User clicked 'connect' in menu");
				clickedConnect(item);
				return true;

			case R.id.roster_menu_form_team:
				Log.d(CLASS, "User clicked 'form team' in menu");
				displayDialog(new FormTeamDialog());
				return true;

			case R.id.roster_menu_add_contact:
				Log.d(CLASS, "User clicked 'add contact' in menu");
				displayDialog(new AddContactDialog());
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

	private void clickedConnect(MenuItem connectMenu) {
		connectMenu.setEnabled(false);
		if (mXMPPService.isAuthenticated()) {
			new DisconnectTask((XMPPService)mXMPPService, new DisconnectHandler()).execute();
		} else {
			new ConnectTask((XMPPService)mXMPPService, new ConnectHandler()).execute();
		}
	}

	private void displayDialog(DialogFragment dialog) {
		dialog.show(getSupportFragmentManager(), null);
	}

	public void enteredTeamName(String teamName) {
		String sanitizedTeamName = teamName.toLowerCase();
		Log.d(CLASS, String.format("Will create team '%s'", sanitizedTeamName));
		final SharedPreferences settings =
				PreferenceManager.getDefaultSharedPreferences(this);
		final String conferenceSrvKey = getString(R.string.preference_conference_server_key);
		final String conferenceSrv = settings.getString(conferenceSrvKey, "");
		new FormTeamTask(mXMPPService, new FormTeamHandler()).execute(sanitizedTeamName, conferenceSrv);
	}

	public void addContact(String contact, String group) {
		new AddContactTask(mXMPPService, new AddContactHandler()).execute(contact, group);
	}

	public void clickedJoinTeam(String team, String userID, String password, String inviter) {
		new JoinTeamTask(mXMPPService, new JoinTeamHandler()).execute(team, userID, password, inviter);
	}

	public void clickedRejectTeam(String team, String inviter) {
		try {
			mXMPPService.declineInvitation(team, inviter, getString(R.string.reason_team_rejection));
		} catch (XMPPException e) {
			String problem = String.format("Error when declining invitation to team '%s': %s", team, e.getMessage());
			Log.e(CLASS, problem);
			Toast.makeText(this, problem, Toast.LENGTH_LONG).show();
		}
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
		private de.teammeet.activities.roster.RosterActivity.ConnectHandler.ConnectProgressDialog mProgressDialog;

		@Override
		public void onPreExecute() {
			showProgressDialog();
		}

		@Override
		public void onTaskCompleted(Void nothing) {
			Log.d(CLASS, "Connect task completed!!");
			dismissProgressDialog();
			invalidateOptionsMenu();

			// broadcast connected
			BroadcastHelper.toggleConnectionStateBroadcast(RosterActivity.this,
														   R.string.broadcast_disconnected,
														   R.string.broadcast_connected);
		}

		@Override
		public void onTaskAborted(Exception e) {
			dismissProgressDialog();
			invalidateOptionsMenu();
			String problem = String.format("Failed to connect to XMPP server: %s", e.getMessage());
			Toast.makeText(RosterActivity.this, problem, Toast.LENGTH_LONG).show();
		}

		private void showProgressDialog() {
			mProgressDialog = new ConnectProgressDialog();
			FragmentManager fm = getSupportFragmentManager();
			mProgressDialog.show(fm, null);
		}

		private void dismissProgressDialog() {
			mProgressDialog.dismiss();
		}

		private class ConnectProgressDialog extends DialogFragment {
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				ProgressDialog dialog = new ProgressDialog(RosterActivity.this);
				dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				dialog.setMessage("Connecting...");
				dialog.setCancelable(false);
				dialog.setIndeterminate(true);
				return dialog;
			}
		}
	}

	private class DisconnectHandler extends BaseAsyncTaskCallback<Void> {
		@Override
		public void onTaskCompleted(Void result) {
			Log.d(CLASS, "you're now disconnected");
			invalidateOptionsMenu();

			// broadcast disconnected
			BroadcastHelper.toggleConnectionStateBroadcast(RosterActivity.this,
														   R.string.broadcast_connected,
														   R.string.broadcast_disconnected);
		}
	}

	private class FormTeamHandler extends BaseAsyncTaskCallback<String> {
		@Override
		public void onTaskCompleted(String teamName) {
			String user_feedback = String.format("Founded team '%s'", teamName);
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

	private class AddContactHandler extends BaseAsyncTaskCallback<String[]> {
		@Override
		public void onTaskCompleted(String[] connection_data) {
			String user_feedback = String.format("You added %s to %s", connection_data[0], connection_data[1]);
			Toast.makeText(RosterActivity.this, user_feedback, Toast.LENGTH_LONG).show();
		}

		@Override
		public void onTaskAborted(Exception e) {
			String problem = String.format("Failed to add contact: %s", e.getMessage());
			Toast.makeText(RosterActivity.this, problem, Toast.LENGTH_LONG).show();
		}
	}

	private class JoinTeamHandler extends BaseAsyncTaskCallback<String> {
		@Override
		public void onTaskCompleted(String teamName) {
			String user_feedback = String.format("Joined team '%s'", StringUtils.parseName(teamName));
			Toast.makeText(RosterActivity.this, user_feedback, Toast.LENGTH_LONG).show();

			Intent newTeam = new Intent(getString(R.string.broadcast_teams_updated));
			sendBroadcast(newTeam);
		}

		@Override
		public void onTaskAborted(Exception e) {
			String problem = String.format("Failed to join team: %s", e.getMessage());
			Toast.makeText(RosterActivity.this, problem, Toast.LENGTH_LONG).show();
		}
	}
}
