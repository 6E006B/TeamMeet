package de.teammeet.activities.chat;

import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class ChatsAdapter extends FragmentPagerAdapter {

	private List<ChatInformation> mChatInformationList;

	public ChatsAdapter(FragmentManager fragmentManager, List<ChatInformation> chatInformationList) {
		super(fragmentManager);
		mChatInformationList = chatInformationList;
	}

	@Override
	public Fragment getItem(int position) {
		ChatInformation chatInfo = mChatInformationList.get(position);
		return ChatFragment.getInstance(chatInfo.getType(), chatInfo.getCounterpart());
	}

	@Override
	public int getCount() {
		return mChatInformationList.size();
	}
}
