package de.teammeet.tasks;

import org.jivesoftware.smack.XMPPException;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import de.teammeet.R;
import de.teammeet.interfaces.IAsyncTaskCallback;
import de.teammeet.interfaces.IXMPPService;
import de.teammeet.services.xmpp.XMPPService;

public class ConnectTask extends BaseAsyncTask<Void, Void, Void> {

	private static final String CLASS = ConnectTask.class.getSimpleName();
	private SherlockFragmentActivity mActivity;
	private ConnectProgressDialog mProgressDialog;

	public ConnectTask(SherlockFragmentActivity activity, IXMPPService service, IAsyncTaskCallback<Void> callback) {
		super(service, callback);
		mActivity = activity;
	}

	@Override
	protected void onPreExecute() {
		showProgressDialog();
	}

	@Override
	protected Void doInBackground(Void... params) {
		XMPPService xmppService = (XMPPService)mService;
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(xmppService);
		try {
			String user =
					settings.getString(xmppService.getString(R.string.preference_user_id_key), "");
			String server =
					settings.getString(xmppService.getString(R.string.preference_server_key), "");
			String passwd =
					settings.getString(xmppService.getString(R.string.preference_password_key), "");
			mService.connect(user, server, passwd);
		} catch (XMPPException e) {
			Log.e(CLASS, "Failed to login: " + e.toString());
			e.printStackTrace();
			mError = e;
			cancel(false);
		} finally {
			dismissProgressDialog();
		}
		return null;
	}

	private void showProgressDialog() {
		mProgressDialog = new ConnectProgressDialog();
		FragmentManager fm = mActivity.getSupportFragmentManager();
		mProgressDialog.show(fm, null);
	}

	private void dismissProgressDialog() {
		mProgressDialog.dismiss();
	}

	private class ConnectProgressDialog extends DialogFragment {
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			ProgressDialog dialog = new ProgressDialog(mActivity);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setMessage("Connecting...");
			dialog.setCancelable(false);
			dialog.setIndeterminate(true);
			return dialog;
		}
	}
}
