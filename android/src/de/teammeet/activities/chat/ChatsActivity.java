package de.teammeet.activities.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import de.teammeet.R;
import de.teammeet.services.xmpp.XMPPService;

public class ChatsActivity extends FragmentActivity implements ViewPager.OnPageChangeListener {

	private static final String CLASS = ChatsActivity.class.getSimpleName();

	private ViewPager mViewPager;
	private ChatsAdapter mPagerAdapter;
	private List<Fragment> mChatFragmentList = new Vector<Fragment>();

	private Map<String, ChatFragment> mChatFragmentsMap = new HashMap<String, ChatFragment>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(CLASS, "creating chats activity");

		// Inflate the layout
		setContentView(R.layout.chats);

		// Intialise ViewPager
		intialiseViewPager();
		
		handleIntent(getIntent());
	}

	@Override
	protected void onResume() {
		Log.d(CLASS, "Resuming chats activity");
		super.onResume();
	}

	@Override
	protected void onPause() {
		Log.d(CLASS, "Pausing chats activity");
		super.onPause();
	}

	/**
	 * Initialise ViewPager
	 */
	private void intialiseViewPager() {

		mPagerAdapter = new ChatsAdapter(super.getSupportFragmentManager(), mChatFragmentList);

		mViewPager = (ViewPager)super.findViewById(R.id.viewpager);
		mViewPager.setAdapter(mPagerAdapter);
		mViewPager.setOnPageChangeListener(this);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		Log.d(CLASS, "handling intent");
		Bundle extras = intent.getExtras();
		if (extras != null) {
			Log.d(CLASS, "extras: " + extras.toString());
		} else {
			Log.d(CLASS, "no extras");
		}
		final int type = intent.getIntExtra(XMPPService.TYPE, 0);
		intent.removeExtra(XMPPService.TYPE);
		final String counterpart = intent.getStringExtra(XMPPService.SENDER);
		intent.removeExtra(XMPPService.SENDER);
		switch (type) {
		case Chat.TYPE_NORMAL_CHAT:
		case Chat.TYPE_GROUP_CHAT:
			Log.d(CLASS, "Intent of chat message");
			handleChatIntent(counterpart, type);
			break;
		default:
			Log.d(CLASS, "Intent of unknown type: " + type);
			break;
		}
	}

	private void handleChatIntent(String counterpart, int type) {
		// TODO Auto-generated method stub
		if (counterpart != null) {
			ChatFragment chatFragment;
			if (!mChatFragmentsMap.containsKey(counterpart)) {
				chatFragment = ChatFragment.getInstance(type, counterpart);
				mChatFragmentsMap.put(counterpart, chatFragment);
				mChatFragmentList.add(chatFragment);
//				AddTab(this, mTabHost, tabSpec, tabInfo);
			} else {
				chatFragment = mChatFragmentsMap.get(counterpart);
			}
			mViewPager.setCurrentItem(mPagerAdapter.getItemPosition(chatFragment), true);
//			mPagerAdapter.getItem(mPagerAdapter.getItemPosition(chatFragment));
		} else {
			Log.e(CLASS, "Intent did not contain a sender of message.");
		}
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onPageSelected(int position) {
		// TODO Auto-generated method stub
	}

}
