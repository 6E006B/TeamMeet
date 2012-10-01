package de.teammeet.activities.teams;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;


public class TabListener<T extends Fragment> implements ActionBar.TabListener {
	private Fragment mFragment;
	private final Activity mActivity;
	private final String mTag;
	private final Class<T> mClass;
	private final int mContainer;
	private final Bundle mArgs;

	/** Constructor used each time a new tab is created.
	  * @param activity  The host Activity, used to instantiate the fragment
	  * @param tag  The identifier tag for the fragment
	  * @param clz  The fragment's Class, used to instantiate the fragment
	  */
	public TabListener(Activity activity, String tag, Class<T> clz, int fragmentContainer, Bundle args) {
		mActivity = activity;
		mTag = tag;
		mClass = clz;
		mContainer = fragmentContainer;
		mArgs = args;
		mFragment = ((FragmentActivity) activity).getSupportFragmentManager().findFragmentByTag(mTag);
	}

	/* The following are each of the ActionBar.TabListener callbacks */

	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		// Check if the fragment is already initialized
		if (mFragment == null) {
			// If not, instantiate and add it to the activity
			mFragment = Fragment.instantiate(mActivity, mClass.getName(), mArgs);
			ft.add(mContainer, mFragment, mTag);
		} else {
			// If it exists, simply attach it in order to show it
			ft.attach(mFragment);
		}
	}

	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		if (mFragment != null) {
			// Detach the fragment, because another one is being attached
			ft.detach(mFragment);
		}
	}

	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// User selected the already selected tab. Usually do nothing.
	}
}
