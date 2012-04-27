package de.teammeet.tasks;

import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;

public class DisconnectTask extends BaseAsyncTask<Void, Void, Void> {

	//private static final String CLASS = ConnectTask.class.getSimpleName();

	public DisconnectTask(IXMPPService service, IAsyncTaskCallback<Void> callback) {
		super(service, callback);
	}

	@Override
	protected Void doInBackground(Void... params) {
		mService.disconnect();
		return null;
	}
}
