package de.teammeet.activities.chat;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import de.teammeet.R;
import de.teammeet.helper.ActionBarHelper;
import de.teammeet.services.xmpp.XMPPService;

public class ChatsActivity extends SherlockFragmentActivity implements ViewPager.OnPageChangeListener {

	private static final String CLASS = ChatsActivity.class.getSimpleName();

	private static final String CHAT_INFORMATION_LIST_KEY = "chat_information_list_key";
	private static final String ACTIVE_CHAT_WINDOW_KEY = "active_chat_window_key";

	private ViewPager mViewPager;
	private ChatsAdapter mPagerAdapter;
	private ArrayList<ChatInformation> mChatInformationList = new ArrayList<ChatInformation>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(CLASS, "creating chats activity");

		// Inflate the layout
		setContentView(R.layout.chats);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		if (savedInstanceState != null) {
			mChatInformationList = savedInstanceState.getParcelableArrayList(CHAT_INFORMATION_LIST_KEY);
		} else {
			Log.d(CLASS, "No instance to restore from.");
		}

		// Intialise ViewPager
		intialiseViewPager();

		if (savedInstanceState != null) {
			mViewPager.setCurrentItem(savedInstanceState.getInt(ACTIVE_CHAT_WINDOW_KEY));
		}

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

		mPagerAdapter = new ChatsAdapter(super.getSupportFragmentManager(), mChatInformationList);

		mViewPager = (ViewPager)super.findViewById(R.id.viewpager);
		mViewPager.setAdapter(mPagerAdapter);
		mViewPager.setOnPageChangeListener(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.d(CLASS, "ChatsActivity.onSaveInstanceState()");
		outState.putParcelableArrayList(ChatsActivity.CHAT_INFORMATION_LIST_KEY,
		                                mChatInformationList);
		outState.putInt(ACTIVE_CHAT_WINDOW_KEY, mViewPager.getCurrentItem());
		super.onSaveInstanceState(outState);
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
			ChatInformation chatInfo = new ChatInformation(type, counterpart);
			Log.d(CLASS, "loading chat fragment for " + counterpart);

			int position = mChatInformationList.indexOf(chatInfo);
			if (position == -1) {
				mChatInformationList.add(chatInfo);
				position = mChatInformationList.indexOf(chatInfo);
			}
			Log.d(CLASS, "setting position to " + position);
			mViewPager.setCurrentItem(position);
			setTitle(counterpart);
		} else {
			Log.e(CLASS, "Intent did not contain a sender of message.");
		}
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
		ChatInformation chatInfo = mChatInformationList.get(position);
		setTitle(chatInfo.getCounterpart());
	}

}
