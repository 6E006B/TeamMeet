package de.teammeet.activities.teams;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import de.teammeet.R;
import de.teammeet.activities.chat.Chat;
import de.teammeet.activities.chat.ChatFragment;
import de.teammeet.activities.chat.TabsAdapter;
import de.teammeet.services.xmpp.XMPPService;

public class TeamActivity extends SherlockFragmentActivity {
	private static String CLASS = TeamActivity.class.getSimpleName();
	private static String SAVED_TAB_KEY = "last_tab";

	public static String SELECT_TAB = "select_tab";
	public static enum Tabs {
		CHAT (0),
		MAP (1);

		public final int position;

		Tabs(int position) {
			this.position = position;
		}
	};

	private String mTeamName;
	private int mSelectTab;


	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(CLASS, "Creating tabbed team activity");

		// Inflate the layout
		setContentView(R.layout.tabbed_roster);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(false);

		// Handle Intent
		handleIntent(getIntent());

		// Initialize ViewPager
		intialiseViewPager(actionBar);

		if (savedInstanceState != null) {
			//set the tab as per the saved state
			actionBar.setSelectedNavigationItem(savedInstanceState.getInt(SAVED_TAB_KEY));
		} else {
			actionBar.setSelectedNavigationItem(mSelectTab);
		}
	}

	private void handleIntent(Intent intent) {
		Log.d(CLASS, "handling intent");
		Bundle extras = intent.getExtras();
		if (extras != null) {
			Log.d(CLASS, "extras: " + extras.toString());
		} else {
			Log.d(CLASS, "no extras");
		}

		mTeamName = intent.getStringExtra(XMPPService.SENDER);
		intent.removeExtra(XMPPService.SENDER);

		mSelectTab = intent.getIntExtra(SELECT_TAB, Tabs.CHAT.position);
	}

	private void intialiseViewPager(ActionBar bar) {
		ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
		TabsAdapter tabsAdapter = new TabsAdapter(this, bar, viewPager);

		Tab tab = bar.newTab();
		tab.setText(R.string.tab_teamchat);
		tab.setIcon(R.drawable.social_group);

		Bundle args = new Bundle();
        args.putInt(XMPPService.TYPE, Chat.TYPE_GROUP_CHAT);
        args.putString(XMPPService.SENDER, mTeamName);
		tabsAdapter.addTab(tab, ChatFragment.class, args);

		tab = bar.newTab();
		tab.setText(R.string.tab_map);
		tab.setIcon(R.drawable.location_map);
		tabsAdapter.addTab(tab, MapFragment.class, null);
	}

	/** (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onSaveInstanceState(android.os.Bundle)
	 */
	protected void onSaveInstanceState(Bundle outState) {
		Log.d(CLASS, "saving instance state...");
		ActionBar bar = getSupportActionBar();
		outState.putInt(SAVED_TAB_KEY, bar.getSelectedNavigationIndex()); //save the tab selected
		super.onSaveInstanceState(outState);
	}
}
