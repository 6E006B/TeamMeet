package de.teammeet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
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
		Log.d(CLASS, String.format("getItem() was called for position %d:", position));
		return mFragments.get(position);
	}

	@Override
	public int getCount() {
		return mFragments.size();
	}
	
	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		Fragment fragment = (Fragment) super.instantiateItem(container, position);
		Log.d(CLASS, String.format("instantiateItem() was called for container %d, position %d:", container.getId(), position));
		Log.d(CLASS, String.format("instantiated fragment %s", fragment));
		
		mFragments.set(position, fragment);
		return fragment;
		
	}
	
	/*
	 * Leak the tag under which the adapter added a fragment to the fragment manager via introspection.
	 */
	public String getFragmentName(int viewId, int index) {
		String name = null;
		Class params[] = new Class[2];
		params[0] = int.class;
		params[1] = int.class;
		
		try {
			Class fragmentPagerAdapter = Class.forName("android.support.v4.app.FragmentPagerAdapter");
			Method makeFragmentName = fragmentPagerAdapter.getDeclaredMethod("makeFragmentName", params);
			makeFragmentName.setAccessible(true);
			name = (String) makeFragmentName.invoke(null, viewId, index);
		} catch (SecurityException e) {
			Log.e(CLASS, String.format("Insecure access to private method 'makeFragmentName()': %s", e.getMessage()));
		} catch (NoSuchMethodException e) {
			Log.e(CLASS, String.format("Couldn't find private method 'makeFragmentName()': %s", e.getMessage()));
		} catch (IllegalArgumentException e) {
			Log.e(CLASS, String.format("Illegal argument to method 'makeFragmentName()': %s", e.getMessage()));
		} catch (IllegalAccessException e) {
			Log.e(CLASS, String.format("Illegal access to private method 'makeFragmentName()': %s", e.getMessage()));
		} catch (InvocationTargetException e) {
			Log.e(CLASS, String.format("Invalid invocation target 'makeFragmentName()': %s", e.getMessage()));
		} catch (ClassNotFoundException e) {
			Log.e(CLASS, "Couldn't find class 'FragmentPagerAdapter'", e);
		}

		return name;
	}
}
