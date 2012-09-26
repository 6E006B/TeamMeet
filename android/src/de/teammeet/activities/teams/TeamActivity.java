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

	private String mTeamName;


	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(CLASS, "Creating tabbed team activity");

		// Inflate the layout
		setContentView(R.layout.tabbed_roster);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(false);

		// Handle Intent
		handleIntent(getIntent());

		// Intialise ViewPager
		intialiseViewPager(actionBar);

		if (savedInstanceState != null) {
			//set the tab as per the saved state
			actionBar.setSelectedNavigationItem(savedInstanceState.getInt(SAVED_TAB_KEY));
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
		//final int type = intent.getIntExtra(XMPPService.TYPE, 0);
		//intent.removeExtra(XMPPService.TYPE);
		mTeamName = intent.getStringExtra(XMPPService.SENDER);
		intent.removeExtra(XMPPService.SENDER);
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

		/*tab = bar.newTab();
		tab.setText(R.string.tab_map);
		tab.setIcon(R.drawable.social_group);
		tabsAdapter.addTab(tab, ChatFragment.class, null);
		*/
	}
}
