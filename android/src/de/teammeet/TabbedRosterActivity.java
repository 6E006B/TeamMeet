package de.teammeet;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;

public class TabbedRosterActivity extends FragmentActivity implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
	private static String CONTACTS_TAB_ID = "contacts_tab";
	private static String TEAMS_TAB_ID = "teams_tab";
	private static String SAVED_TAB_KEY = "last_tab";
	private static int CONTACT_FRAGMENT_POS = 0;
	private static int TEAMS_FRAGMENT_POS = 1;
	
	private TabHost mTabHost;
	private ViewPager mViewPager;
	private HashMap<String, TabInfo> mapTabInfo = new HashMap<String, TabbedRosterActivity.TabInfo>();
	private RosterAdapter mPagerAdapter;
	
	
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
		// Inflate the layout
		setContentView(R.layout.tabbed_roster);
		// Initialise the TabHost
		this.initialiseTabHost(savedInstanceState);
		if (savedInstanceState != null) {
			mTabHost.setCurrentTabByTag(savedInstanceState.getString(SAVED_TAB_KEY)); //set the tab as per the saved state
		}
		// Intialise ViewPager
		this.intialiseViewPager();
		
		ContactsFragment contacts = (ContactsFragment) mPagerAdapter.getItem(CONTACT_FRAGMENT_POS);
		contacts.setIntent(getIntent());
	}

	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		ContactsFragment contacts = (ContactsFragment) mPagerAdapter.getItem(CONTACT_FRAGMENT_POS);
		contacts.setIntent(intent);
	}
	
	/** (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onSaveInstanceState(android.os.Bundle)
	 */
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(SAVED_TAB_KEY, mTabHost.getCurrentTabTag()); //save the tab selected
		super.onSaveInstanceState(outState);
	}

	/**
	 * Initialise ViewPager
	 */
	private void intialiseViewPager() {

		List<Fragment> fragments = new Vector<Fragment>();
		fragments.add(Fragment.instantiate(this, ContactsFragment.class.getName()));
		fragments.add(Fragment.instantiate(this, Teams.class.getName()));
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
	public void onPageScrollStateChanged(int state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}
}
