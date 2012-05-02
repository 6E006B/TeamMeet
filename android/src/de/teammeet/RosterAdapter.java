package de.teammeet;

import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

public class RosterAdapter extends FragmentPagerAdapter {

	private static final String CLASS = RosterAdapter.class.getSimpleName();
	private List<Fragment> mFragments;
	
	public RosterAdapter(FragmentManager fm, List<Fragment> fragments) {
		super(fm);
		mFragments = fragments;
	}

	@Override
	public Fragment getItem(int position) {
		return mFragments.get(position);
	}

	@Override
	public int getCount() {
		return mFragments.size();
	}

	public Fragment getFragment(ViewGroup container, int position) {
		return (Fragment) super.instantiateItem(container, position);
	}
}
