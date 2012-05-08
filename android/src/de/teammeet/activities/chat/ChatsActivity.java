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

	private ActionBar mActionBar;
	private ViewPager mViewPager;
	private TabsAdapter mTabsAdapter;
	private ArrayList<ChatInformation> mChatInformationList = new ArrayList<ChatInformation>();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(CLASS, "creating chats activity");

		// Inflate the layout
		setContentView(R.layout.chats);
		mActionBar = getSupportActionBar();
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		mActionBar.setDisplayShowTitleEnabled(false);

		if (savedInstanceState != null) {
			mChatInformationList = savedInstanceState.getParcelableArrayList(CHAT_INFORMATION_LIST_KEY);
		} else {
			Log.d(CLASS, "No instance to restore from.");
		}

		intialiseViewPager();

		if (savedInstanceState != null) {
			for (ChatInformation chatInfo : mChatInformationList) {
				addTab(chatInfo);
			}
//			mViewPager.setCurrentItem(savedInstanceState.getInt(ACTIVE_CHAT_WINDOW_KEY));
			mActionBar.setSelectedNavigationItem(savedInstanceState.getInt(ACTIVE_CHAT_WINDOW_KEY));
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
		mViewPager = (ViewPager)super.findViewById(R.id.viewpager);
		mViewPager.setOnPageChangeListener(this);
		mTabsAdapter = new TabsAdapter(this, mActionBar, mViewPager);
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
		if (counterpart != null) {
			ChatInformation chatInfo = new ChatInformation(type, counterpart);
			Log.d(CLASS, "loading chat fragment for " + counterpart);

			int position = mChatInformationList.indexOf(chatInfo);
			if (position == -1) {
				mChatInformationList.add(chatInfo);
				position = mChatInformationList.indexOf(chatInfo);
				addTab(chatInfo);
			}
			Log.d(CLASS, "setting position to " + position);
			mViewPager.setCurrentItem(position);
		} else {
			Log.e(CLASS, "Intent did not contain a sender of message.");
		}
	}

	private void addTab(ChatInformation chatInfo) {
		Bundle args = new Bundle();
        args.putInt(XMPPService.TYPE, chatInfo.getType());
        args.putString(XMPPService.SENDER, chatInfo.getCounterpart());
		mTabsAdapter.addTab(mActionBar.newTab().setText(chatInfo.getUsername()),
		                    ChatFragment.class, args);
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
//		ChatInformation chatInfo = mChatInformationList.get(position);
		mActionBar.setSelectedNavigationItem(position);
//		setTitle(chatInfo.getCounterpart());
	}
}
