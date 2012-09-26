package de.teammeet.activities.teams;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import de.teammeet.R;
import de.teammeet.activities.chat.Chat;
import de.teammeet.activities.chat.ChatFragment;
import de.teammeet.helper.ActionBarHelper;
import de.teammeet.services.xmpp.XMPPService;

public class TeamActivity extends SherlockFragmentActivity {
	private static String CLASS = TeamActivity.class.getSimpleName();
	private static String SAVED_TAB_KEY = "last_tab";

	public static String SELECT_TAB = "select_tab";
	public static enum Tabs {
		CHAT (0, "chat_tab"),
		MAP (1, "map_tab");

		public final int position;
		public final String tag;

		Tabs(int position, String tag) {
			this.position = position;
			this.tag = tag;
		}
	};

	private String mTeamName;
	private int mSelectTab;


	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(CLASS, "Creating tabbed team activity");

		// Inflate the layout
		setContentView(R.layout.team_activity);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(false);

		// Handle Intent
		handleIntent(getIntent());

		// Create tabs
		createTabs(actionBar);

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

	private void createTabs(ActionBar bar) {
		Bundle chatArgs = new Bundle();
		chatArgs.putInt(XMPPService.TYPE, Chat.TYPE_GROUP_CHAT);
		chatArgs.putString(XMPPService.SENDER, mTeamName);

		TabListener<ChatFragment> chatTabListener = new TabListener<ChatFragment>(this,
																				  Tabs.CHAT.tag,
																				  ChatFragment.class,
																				  R.id.team_tab,
																				  chatArgs);
		TabListener<MapFragment> mapTabListener = new TabListener<MapFragment>(this,
																			   Tabs.MAP.tag,
																			   MapFragment.class,
																			   R.id.team_tab,
																			   null);

		Tab chatTab = bar.newTab();
		chatTab.setText(R.string.tab_teamchat);
		chatTab.setIcon(R.drawable.social_group);
		chatTab.setTabListener(chatTabListener);

		Tab mapTab = bar.newTab();
		mapTab.setText(R.string.tab_map);
		mapTab.setIcon(R.drawable.location_map);
		mapTab.setTabListener(mapTabListener);

		bar.addTab(chatTab);
		bar.addTab(mapTab);
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			ActionBarHelper.navigateUpInHierarchy(this);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
