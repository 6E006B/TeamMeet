package de.teammeet.activities.roster;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

public class RosterAdapter extends FragmentPagerAdapter {

	private static final int NUM_FRAGMENTS = 2;
	
	public static final int CONTACTS_FRAGMENT_POS = 0;
	public static final int TEAMS_FRAGMENT_POS = 1;

	public RosterAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int position) {
		Fragment newFragment = null;
		
		switch (position) {
		case CONTACTS_FRAGMENT_POS:
			newFragment = new ContactsFragment();
			break;
		case TEAMS_FRAGMENT_POS:
			newFragment = new TeamsFragment();
			break;
		}
		
		return newFragment;
	}

	@Override
	public int getCount() {
		return NUM_FRAGMENTS;
	}

	public Fragment getFragment(ViewGroup container, int position) {
		return (Fragment) super.instantiateItem(container, position);
	}
}
