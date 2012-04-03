/**
 *    Copyright 2012 Daniel Kreischer, Christopher Holm, Christopher Schwardt
 *
 *    This file is part of TeamMeet.
 *
 *    TeamMeet is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    TeamMeet is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with TeamMeet.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.teammeet.helper;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class ToastDisposerSingleton extends Activity {
	class ExecuteRunnableHandler extends Handler {
		@Override
		public void handleMessage(final Message msg) {
			// Log.e(CLASS + ".ExecuteRunnableHandler", "handleMessage");
			if (msg.obj != null) {
				Log.e(CLASS + ".ExecuteRunnableHandler", " m_isPause: " + mIsPaused + ", mViewID: " +
						mViewID + ", getViewID(): " + ((ToastRunnable) msg.obj).getViewID());
				if (!mIsPaused && ((ToastRunnable) msg.obj).getViewID() == mViewID) {
					((ToastRunnable) msg.obj).run();
				} else {
					Log.e(CLASS + ".ExecuteRunnableHandler", "pausing message:\t\"" +
							((ToastRunnable) msg.obj).getMessage() + "\"");
					mPausedMessages.add((ToastRunnable) msg.obj);
				}
			}
			super.handleMessage(msg);
		}
	};

	private static String					CLASS			= "ToastSingleton";
	private static ToastDisposerSingleton	mInstance		= null;
	private static boolean					mIsPaused		= false;
	private static Context					mContext		= null;
	private static int						mViewID			= 0;

	private ArrayList<ToastRunnable>		mPausedMessages	= null;
	private ExecuteRunnableHandler			mMessageHandler	= null;
	private long							mNextMessagesAt	= 0;

	private ToastDisposerSingleton() {
		mPausedMessages = new ArrayList<ToastRunnable>();
		mMessageHandler = new ExecuteRunnableHandler();
	}

	/*
	 * Singleton pattern, setting new application context
	 * Only call once in a View or messages are discarded!
	 */
	public static ToastDisposerSingleton getInstance(Context contextWrapper) {
		// Log.e(CLASS, "getInstance(Context)");
		mContext = contextWrapper;
		mViewID++; // don't show the old pending messages
		return getInstance();
	}

	// Singleton pattern
	private static ToastDisposerSingleton getInstance() {
		// Log.e(CLASS, "getInstance()");
		if (mContext == null) {
			throw new NullPointerException();
		}
		if (mInstance == null) {
			mInstance = new ToastDisposerSingleton();
		}
		return mInstance;
	}

	/*
	 * Adds a toast Toast.LENGTH_SHORT long
	 */
	public void addShortToast(final String message) {
		addToast(message, Toast.LENGTH_SHORT);
	}

	/*
	 * Adds a toast Toast.LENGTH_LONG long
	 */
	public void addLongToast(final String message) {
		addToast(message, Toast.LENGTH_LONG);
	}

	// supplements messages to the queue
	private void addToast(final String message, int duration) {
		ToastRunnable toastRunnable = new ToastRunnable(mContext, message, duration, mViewID);
		enqueueRunnable(toastRunnable);
	}

	private void enqueueRunnable(ToastRunnable toastRunnable) {
		Log.e(CLASS, "enqueueRunnable, added message:\t\"" + toastRunnable.getMessage() + "\" (" +
				toastRunnable.getDuration() + " ms) at " + mNextMessagesAt);

		final Message toastMessage = mMessageHandler.obtainMessage(mViewID, toastRunnable);

		if (SystemClock.uptimeMillis() < mNextMessagesAt) {
			// let message be processed after the last in queue
			mMessageHandler.sendMessageAtTime(toastMessage, mNextMessagesAt);
			mNextMessagesAt += toastRunnable.getDuration();
		} else {
			// all messages have been handled, send next now
			mMessageHandler.sendMessage(toastMessage);
			mNextMessagesAt = SystemClock.uptimeMillis() + toastRunnable.getDuration();
		}
	}

	// discard all processing and reset variables
	public void clearToasts() {
		Log.e(CLASS, "clearTosts");
		mMessageHandler.removeMessages(0);
		mNextMessagesAt = 0;
	}

	public void pauseMessages() {
		Log.e(CLASS, "pauseMessages");
		mIsPaused = true;
	}

	public void resumeMessages() {
		Log.e(CLASS, "resumeMessages");
		mIsPaused = false;
		mNextMessagesAt = SystemClock.uptimeMillis();

		// re-enqueue all toasts that were not shown on pause
		for (ToastRunnable r : mPausedMessages) {
			enqueueRunnable(new ToastRunnable(r.getContext(), r.getMessage(), r.getDuration(), mViewID));
		}
		mPausedMessages.clear();
	}
}