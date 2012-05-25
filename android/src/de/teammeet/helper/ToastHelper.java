package de.teammeet.helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * A simple helper to display `Toast`s from a thread with a `Context`
 * but no `Activity` around
 */
public class ToastHelper extends Handler {

	private static final String CLASS = ToastHelper.class.getSimpleName();

	private static ToastHelper mInstance = null;

	private Toast mToast;


	public static void initialize(Context context) {
		mInstance = new ToastHelper(context);
	}

	public static ToastHelper getInstance() {
		ToastHelper instance = null;

		if (mInstance != null) {
			instance = mInstance;
		} else {
			Log.e(CLASS, "ToastHelper is uninitialized, toasts will not show!");
			instance = new ToastHelper(null);
		}

		return instance;
	}

	private ToastHelper(Context context) {
		super(Looper.getMainLooper());
		mToast = Toast.makeText(context, null, Toast.LENGTH_LONG);
	}

	public void toast(final String msg) {
		this.post(new Runnable() {
			@Override
			public void run() {
				mToast.setText(msg);
				mToast.show();
			}
		});
	}
}
