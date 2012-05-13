package de.teammeet.helper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.util.Log;

public class BroadcastHelper {
	private static final String CLASS = BroadcastHelper.class.getSimpleName();
	
	public static BroadcastReceiver getBroadcastReceiverInstance(Fragment parent, Class<? extends BroadcastReceiver> type,
			 											   		 int category, int action) {
		BroadcastReceiver instance = createBroadcastReceiver(parent, type);
		Activity activity = parent.getActivity();
		
		IntentFilter filter = new IntentFilter(activity.getString(action));
		filter.addCategory(activity.getString(category));
		
		activity.registerReceiver(instance, filter);
		
		return instance;
	}
		
	public static BroadcastReceiver getBroadcastReceiverInstance(Fragment parent, Class<? extends BroadcastReceiver> type,
				   										  		 int action) {
		BroadcastReceiver instance = createBroadcastReceiver(parent, type);
		Activity activity = parent.getActivity();
		
		IntentFilter filter = new IntentFilter(activity.getString(action));
		
		activity.registerReceiver(instance, filter);
		
		return instance;
	}
	
	private static BroadcastReceiver createBroadcastReceiver(Fragment parent, Class<? extends BroadcastReceiver> type) {
		BroadcastReceiver instance = null;
	
		try {
			Constructor<? extends BroadcastReceiver> constructor = type.getDeclaredConstructor(parent.getClass());
			constructor.setAccessible(true);
			instance = constructor.newInstance(parent);
		} catch (NoSuchMethodException e) {
			Log.e(CLASS, String.format("Could not fetch constructor for broadcast receiver '%s': %s",
			type.getName(), e.getMessage()));
		} catch (IllegalArgumentException e) {
			Log.e(CLASS, String.format("Wrong argument when creating broadcast receiver '%s': %s",
			type.getName(), e.getMessage()));
		} catch (InvocationTargetException e) {
			Log.e(CLASS, String.format("Error in constructor of '%s': %s",
			type.getName(), e.getMessage()));
		} catch (java.lang.InstantiationException e) {
			Log.e(CLASS, String.format("Could not instantiate broadcast receiver type '%s': %s",
			type.getName(), e.getMessage()));
		} catch (IllegalAccessException e) {
			Log.e(CLASS, String.format("Instantiation of receiver of type '%s' denied: %s",
			type.getName(), e.getMessage()));
		}
			
		return instance;
	}
}
