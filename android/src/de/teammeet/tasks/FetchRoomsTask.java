package de.teammeet.tasks;

import java.util.Set;

import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;

public class FetchRoomsTask extends BaseAsyncTask<Void, Void, Set<String>> {

	private static final String CLASS = FetchRoomsTask.class.getSimpleName();

	public FetchRoomsTask(IXMPPService service, IAsyncTaskCallback<Set<String>> callback) {
		super(service, callback);
	}

	@Override
	protected Set<String> doInBackground(Void... params) {

		return mService.getRooms();
	}
}
