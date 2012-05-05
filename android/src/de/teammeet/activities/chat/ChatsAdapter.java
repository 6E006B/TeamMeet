package de.teammeet.activities.chat;

import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class ChatsAdapter extends FragmentPagerAdapter {

	private List<ChatFragment> mFragments;

	public ChatsAdapter(FragmentManager fragmentManager, List<ChatFragment> fragments) {
		super(fragmentManager);
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
}
