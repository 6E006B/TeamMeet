package de.teammeet.interfaces;

public interface AsyncTaskCallback<T> {
	
	public void onTaskCompleted(T result);
	
}
