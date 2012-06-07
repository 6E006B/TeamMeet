package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.util.Log;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;

public class RemoveMateTask extends BaseAsyncTask<String, Void, String> {

	private static final String CLASS = RemoveMateTask.class.getSimpleName();

	public RemoveMateTask(IXMPPService service, IAsyncTaskCallback<String> callback) {
		super(service, callback);
	}

	@Override
	protected String doInBackground(String... params) {
		final String contact = params[0];
		
		try {
			mService.removeContact(contact);
		} catch (XMPPException e) {
			Log.e(CLASS, String.format("Failed to remove contact '%s': %s", contact, e.getMessage()));
			mError = e;
			cancel(false);
		}

		return contact;
	}
}
