package de.teammeet.helper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.util.Log;
import de.teammeet.R;

public class BroadcastHelper {
	private static final String CLASS = BroadcastHelper.class.getSimpleName();

	public static void toggleConnectionStateBroadcast(Context context, int oldStateAction, int newStateAction) {
		String broadcastCategory = context.getString(R.string.broadcast_connection_state);
		String oldAction = context.getString(oldStateAction);
		String newAction = context.getString(newStateAction);

		Log.d(CLASS, "Cutting off old connection broadcast");
		Intent intent = new Intent();
		intent.addCategory(broadcastCategory);
		intent.setAction(oldAction);
		context.removeStickyBroadcast(intent);

		Log.d(CLASS, "Turning on new connection broadcast");
		intent = new Intent();
		intent.addCategory(broadcastCategory);
		intent.setAction(newAction);
		context.sendStickyBroadcast(intent);
	}

	public static BroadcastReceiver getBroadcastReceiverInstance(Activity parent,
			Class<? extends BroadcastReceiver> type, int category, int action) {
		BroadcastReceiver instance = createBroadcastReceiver(parent, type);

		IntentFilter filter = new IntentFilter(parent.getString(action));
		filter.addCategory(parent.getString(category));

		parent.registerReceiver(instance, filter);

		return instance;
	}

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
	
	private static BroadcastReceiver createBroadcastReceiver(Object parent, Class<? extends BroadcastReceiver> type) {
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
