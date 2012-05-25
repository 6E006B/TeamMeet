package de.teammeet.helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * A simple helper to display `Toast`s from a thread with a `Context`
 * but no `Activity` around
 */
public class ToastHelper extends Handler {

	private static Context mContext = null;


	public static void initialize(Context context) {
		mContext = context;
	}

	public ToastHelper() {
		super(Looper.getMainLooper());
	}

	public void toast(final String msg) {
		this.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
			}
		});
	}
}
