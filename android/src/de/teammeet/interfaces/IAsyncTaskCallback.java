package de.teammeet.interfaces;

public interface IAsyncTaskCallback<T> {
	
	public void onTaskCompleted(T result);

	public void onTaskAborted(Exception e);
}
