package de.teammeet.helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * A simple helper to display `Toast`s from a thread with a `Context`
 * but no `Activity` around
 */
public class ToasterHelper extends Handler {

	private Context mContext;

	public ToasterHelper(final Context context) {
		super(Looper.getMainLooper());
		mContext = context;
	}
	
	public void toast(final String msg) {
		this.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
			}
		});
	}
}
