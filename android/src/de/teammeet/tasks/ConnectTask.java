package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.util.Log;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;

public class ConnectTask extends BaseAsyncTask<Void, Void, Void> {

	private static final String CLASS = ConnectTask.class.getSimpleName();

	public ConnectTask(IXMPPService service, IAsyncTaskCallback<Void> callback) {
		super(service, callback);
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		try {
			mService.connect();
		} catch (XMPPException e) {
			Log.e(CLASS, "Failed to login: " + e.toString());
			e.printStackTrace();
			mError = e;
			cancel(false);
		}
		return null;
	}
}
