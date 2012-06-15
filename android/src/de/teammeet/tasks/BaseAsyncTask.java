package de.teammeet.tasks;

import android.os.AsyncTask;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;

public abstract class BaseAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

	protected IXMPPService mService;
	protected IAsyncTaskCallback<Result> mCallback;
	protected Exception mError;
	
	public BaseAsyncTask(IXMPPService service, IAsyncTaskCallback<Result> callback) {
		mService = service;
		mCallback = callback;
	}

	@Override
	protected void onPreExecute() {
		mCallback.onPreExecute();
	}

	@Override
	protected void onPostExecute(Result result) {
		mCallback.onTaskCompleted(result);
	}
	
	@Override
	protected void onCancelled() {
		mCallback.onTaskAborted(mError);
	}
}
