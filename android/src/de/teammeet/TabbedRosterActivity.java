package de.teammeet;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.jivesoftware.smack.XMPPException;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.Toast;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.tasks.BaseAsyncTaskCallback;
import de.teammeet.tasks.ConnectTask;
import de.teammeet.tasks.CreateGroupTask;
import de.teammeet.tasks.DisconnectTask;
import de.teammeet.tasks.FetchRosterTask;
import de.teammeet.xmpp.XMPPService;

public class TabbedRosterActivity extends FragmentActivity implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
	private static String CLASS = TabbedRosterActivity.class.getSimpleName();
	private static String CONTACTS_TAB_ID = "contacts_tab";
	private static String TEAMS_TAB_ID = "teams_tab";
	private static String SAVED_TAB_KEY = "last_tab";
	private static int CONTACTS_FRAGMENT_POS = 0;
	private static int TEAMS_FRAGMENT_POS = 1;
	
	private IXMPPService mXMPPService = null;
	private XMPPServiceConnection mXMPPServiceConnection = new XMPPServiceConnection();
	private Intent mCurrentIntent = null;
	
	private TabHost mTabHost;
	private ViewPager mViewPager;
	private HashMap<String, TabInfo> mapTabInfo = new HashMap<String, TabbedRosterActivity.TabInfo>();
	private RosterAdapter mPagerAdapter;
	
	
	private class XMPPServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(CLASS, "RosterActivity.XMPPServiceConnection.onServiceConnected('" + className + "')");
			mXMPPService = ((XMPPService.LocalBinder) binder).getService();

			if (mXMPPService.isAuthenticated()) {
				// spawn `FetchRosterTask` but have it handled in the `ContactsFragment`
				ContactsFragment contacts = (ContactsFragment) mPagerAdapter.getItem(CONTACTS_FRAGMENT_POS);
				new FetchRosterTask(mXMPPService, contacts.new FetchRosterHandler()).execute();
			}
			if (mCurrentIntent != null) {
				handleIntent(mCurrentIntent);
			} else {
				Log.d(CLASS, "Skipping handling of intent since it hasn't been set yet *lazy*");
			}
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
			// spawn `FetchRosterTask` but have it handled in the `ContactsFragment`
			ContactsFragment contacts = (ContactsFragment) mPagerAdapter.getItem(CONTACTS_FRAGMENT_POS);
			new FetchRosterTask(mXMPPService, contacts.new FetchRosterHandler()).execute();
		}
		
		@Override
		public void onTaskAborted(Exception e) {
			String problem = String.format("Failed to connect to XMPP server: %s", e.getMessage());
			Toast.makeText(TabbedRosterActivity.this, problem, Toast.LENGTH_LONG).show();
		}
	}

	private class DisconnectHandler extends BaseAsyncTaskCallback<Void> {
		@Override
		public void onTaskCompleted(Void result) {
			Log.d(CLASS, "you're now disconnected");
			//TODO notify contacts fragment
			((ContactsFragment) mPagerAdapter.getItem(CONTACTS_FRAGMENT_POS)).handleDisconnect();
		}
	}
	
	private class FormTeamHandler extends BaseAsyncTaskCallback<String[]> {
		@Override
		public void onTaskCompleted(String[] connection_data) {
			String user_feedback = String.format("Founded team '%s'", connection_data[0]);
			Toast.makeText(TabbedRosterActivity.this, user_feedback, Toast.LENGTH_LONG).show();
		}
	
		@Override
		public void onTaskAborted(Exception e) {
			String problem = String.format("Failed to form team: %s", e.getMessage());
			Toast.makeText(TabbedRosterActivity.this, problem, Toast.LENGTH_LONG).show();
		}
	}
	

	/**
	 * Maintains extrinsic info of a tab's construct
	 */
	private class TabInfo {
		private String tag;
		private Class<?> clss;
		private Bundle args;
		private Fragment fragment;
		TabInfo(String tag, Class<?> clazz, Bundle args) {
			this.tag = tag;
			this.clss = clazz;
			this.args = args;
		}

	}
	/**
	 * A simple factory that returns dummy views to the Tabhost
	 * @author mwho
	 */
	class TabFactory implements TabContentFactory {

		private final Context mContext;

		/**
		 * @param context
		 */
		public TabFactory(Context context) {
			mContext = context;
		}

		/** (non-Javadoc)
		 * @see android.widget.TabHost.TabContentFactory#createTabContent(java.lang.String)
		 */
		public View createTabContent(String tag) {
			View v = new View(mContext);
			v.setMinimumWidth(0);
			v.setMinimumHeight(0);
			return v;
		}

	}
	/** (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
	 */
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(CLASS, "creating tabbed roster activity");
		
		// Inflate the layout
		setContentView(R.layout.tabbed_roster);
		
		// Initialise the TabHost
		this.initialiseTabHost(savedInstanceState);
		if (savedInstanceState != null) {
			mTabHost.setCurrentTabByTag(savedInstanceState.getString(SAVED_TAB_KEY)); //set the tab as per the saved state
		}
		
		// Intialise ViewPager
		this.intialiseViewPager();
		
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
		outState.putString(SAVED_TAB_KEY, mTabHost.getCurrentTabTag()); //save the tab selected
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

		List<Fragment> fragments = new Vector<Fragment>();
		fragments.add(CONTACTS_FRAGMENT_POS, Fragment.instantiate(this, ContactsFragment.class.getName()));
		fragments.add(TEAMS_FRAGMENT_POS, Fragment.instantiate(this, Teams.class.getName()));
		this.mPagerAdapter  = new RosterAdapter(super.getSupportFragmentManager(), fragments);

		this.mViewPager = (ViewPager)super.findViewById(R.id.viewpager);
		this.mViewPager.setAdapter(this.mPagerAdapter);
		this.mViewPager.setOnPageChangeListener(this);
	}

	/**
	 * Initialise the Tab Host
	 */
	private void initialiseTabHost(Bundle args) {
		mTabHost = (TabHost)findViewById(android.R.id.tabhost);
		mTabHost.setup();
		TabInfo tabInfo = null;
		TabbedRosterActivity.AddTab(this, this.mTabHost, this.mTabHost.newTabSpec(CONTACTS_TAB_ID).setIndicator(getString(R.string.tab_contacts)), ( tabInfo = new TabInfo(CONTACTS_TAB_ID, Contacts.class, args)));
		this.mapTabInfo.put(tabInfo.tag, tabInfo);
		TabbedRosterActivity.AddTab(this, this.mTabHost, this.mTabHost.newTabSpec(TEAMS_TAB_ID).setIndicator(getString(R.string.tab_teams)), ( tabInfo = new TabInfo(TEAMS_TAB_ID, Teams.class, args)));
		this.mapTabInfo.put(tabInfo.tag, tabInfo);
		// Default to first tab
		//this.onTabChanged("Tab1");
		//
		mTabHost.setOnTabChangedListener(this);
	}

	/**
	 * Add Tab content to the Tabhost
	 * @param activity
	 * @param tabHost
	 * @param tabSpec
	 * @param clss
	 * @param args
	 */
	private static void AddTab(TabbedRosterActivity activity, TabHost tabHost, TabHost.TabSpec tabSpec, TabInfo tabInfo) {
		// Attach a Tab view factory to the spec
		tabSpec.setContent(activity.new TabFactory(activity));
		tabHost.addTab(tabSpec);
	}

	public void onTabChanged(String tag) {
		int pos = this.mTabHost.getCurrentTab();
		this.mViewPager.setCurrentItem(pos);
	}

	@Override
	public void onPageSelected(int position) {
		this.mTabHost.setCurrentTab(position);
	}

	@Override
	public void onPageScrollStateChanged(int state) {}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {}
	
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
		// cleanup the extras so that this is only executed once, not every time the activity is
		// brought to foreground again
		intent.removeExtra(XMPPService.ROOM);
		intent.removeExtra(XMPPService.INVITER);
		intent.removeExtra(XMPPService.REASON);
		intent.removeExtra(XMPPService.PASSWORD);
		intent.removeExtra(XMPPService.FROM);
		Log.d(CLASS, String.format("room: '%s' inviter: '%s' reason: '%s' password: '%s' from: '%s'", room, inviter, reason, password, from));
		if (room != null && inviter != null && reason != null && from != null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Group Invitation");
			builder.setMessage(String.format("%s wants you to join '%s':\n%s",
			                                 inviter, room, reason));
			builder.setCancelable(false);
			builder.setPositiveButton("Join", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.dismiss();
			                SharedPreferences settings =
			                		getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
			                final String userID =
			                		settings.getString(SettingsActivity.SETTING_XMPP_USER_ID,
			                		                   "anonymous");
			                try {
								mXMPPService.joinRoom(room, userID, password);
							} catch (XMPPException e) {
								e.printStackTrace();
								Log.e(CLASS, "Unable to join room.");
								// TODO show the user
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.roster, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Log.d(CLASS, String.format("preparing roster options menu: %d", menu.size()));

		MenuItem connectMenu = menu.findItem(R.id.roster_menu_connect);
		MenuItem formTeamMenu = menu.findItem(R.id.roster_menu_form_team);

		Log.d(CLASS, String.format("connect menu is '%s'", connectMenu.toString()));
		
		Resources res = getResources();
		int connectTitle = R.string.roster_menu_connect;
		CharSequence connectTitleCondensed = res.getString(R.string.roster_menu_connect_condensed);
		boolean enableConnect = false;
		boolean enableFormTeam = false;
		
		if (mXMPPService != null) {
			enableConnect = true;
			
			if (mXMPPService.isAuthenticated()) {
			Log.d(CLASS, "setting menu option to 'disconnect'");
			connectTitle = R.string.roster_menu_disconnect;
			connectTitleCondensed = res.getString(R.string.roster_menu_disconnect_condensed);
			enableFormTeam = true;
			}
		}
		connectMenu.setTitle(connectTitle);
		connectMenu.setTitleCondensed(connectTitleCondensed);
		connectMenu.setEnabled(enableConnect);
		formTeamMenu.setEnabled(enableFormTeam);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {

		switch (item.getItemId()) {
			case R.id.roster_menu_connect:
				Log.d(CLASS, "User clicked 'connect' in menu");
				clickedConnect();
				break;

			case R.id.roster_menu_show_map:
				Log.d(CLASS, "User clicked 'map' in menu");
				clickedMap();
				break;

			case R.id.roster_menu_form_team:
				Log.d(CLASS, "User clicked 'form team' in menu");
				clickedFormTeam();
				break;

			case R.id.roster_menu_settings:
				Log.d(CLASS, "User clicked 'form team' in menu");
				clickedSettings();
				break;

			case R.id.roster_menu_exit:
				Log.d(CLASS, "User clicked 'exit' in menu");
				clickedExit();
				break;

			default:
				break;
		}
		return super.onOptionsItemSelected(item);
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

	private void clickedConnect() {
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
		SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
		String conferenceSrv = settings.getString(SettingsActivity.SETTING_XMPP_CONFERENCE_SERVER, "");
		new CreateGroupTask(mXMPPService, new FormTeamHandler()).execute(teamName, conferenceSrv);
	}
}
