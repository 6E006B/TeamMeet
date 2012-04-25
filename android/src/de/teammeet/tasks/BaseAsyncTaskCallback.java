package de.teammeet.tasks;

import android.util.Log;
import de.teammeet.interfaces.IAsyncTaskCallback;

public class BaseAsyncTaskCallback<T> implements IAsyncTaskCallback<T> {

	private static final String CLASS = BaseAsyncTaskCallback.class.getSimpleName();
	
	@Override
	public void onTaskCompleted(T result) {
		Log.d(CLASS, String.format("%s completed successfully", getTaskType()));
	}
	
	private String getTaskType() {
		String className = new Throwable().fillInStackTrace().getStackTrace()[3].getClassName();
		return className.substring(className.lastIndexOf('.') + 1);
	}

}
