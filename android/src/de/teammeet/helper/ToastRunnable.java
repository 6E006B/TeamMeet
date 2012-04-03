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

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class ToastRunnable implements Runnable {
	private static final String	CLASS	= "ToastRunnable";
	private String				mMessage;
	private int					mDuration;
	private int					mViewID;
	private Context				mContext;

	public ToastRunnable(Context context, String message, int duration, int viewID) {
		mContext = context;
		mMessage = message;
		mDuration = duration;
		mViewID = viewID;
	}

	public int getViewID() {
		return mViewID;
	}

	public void setViewID(int viewID) {
		mViewID = viewID;
	}

	@Override
	public void run() {
		Log.e(CLASS + ".Runnable.run()", " processed message:\t\"" + mMessage + "\"");
		Toast.makeText(mContext, mMessage, mDuration).show();
	}

	public String getMessage() {
		return mMessage;
	}

	public void setMessage(String mMessage) {
		this.mMessage = mMessage;
	}

	public int getDuration() {
		return mDuration;
	}

	public void setDuration(int mDuration) {
		this.mDuration = mDuration;
	}

	public Context getContext() {
		return mContext;
	}

	public void setContext(Context mContext) {
		this.mContext = mContext;
	}
}
