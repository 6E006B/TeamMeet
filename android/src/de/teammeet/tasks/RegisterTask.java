package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.util.Log;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;

public class RegisterTask extends BaseAsyncTask<String, Void, String[]> {

	private static final String CLASS = RegisterTask.class.getSimpleName();

	public RegisterTask(IXMPPService service, IAsyncTaskCallback<String[]> callback) {
		super(service, callback);
	}

	@Override
	protected String[] doInBackground(String... params) {

		String server = params[0];
		String username = params[1];
		String password = params[2];
		String[] conn_data = params;

		try {
			mService.registerAccount(server, username, password);
		} catch (XMPPException e) {
			Log.e(CLASS, String.format("Failed to register %s: %s", username, e.getMessage()));
			mError = e;
			cancel(false);
		}

		return conn_data;
	}
}
