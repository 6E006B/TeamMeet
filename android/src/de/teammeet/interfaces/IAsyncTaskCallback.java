package de.teammeet.interfaces;

public interface IAsyncTaskCallback<T> {
	
	public void onTaskCompleted(T result);
	
}
