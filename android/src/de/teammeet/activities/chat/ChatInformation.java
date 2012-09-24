package de.teammeet.activities.chat;

import org.jivesoftware.smack.util.StringUtils;

import android.os.Parcel;
import android.os.Parcelable;

public class ChatInformation implements Parcelable {

	private int mType;
	private String mCounterpart;

	public ChatInformation(int type, String counterpart) {
		mType = type;
		if (mType == Chat.TYPE_NORMAL_CHAT) {
			mCounterpart = Chat.getUsernameAndServer(counterpart);
		} else {
			mCounterpart = counterpart;
		}
	}

	public int getType() {
		return mType;
	}

	public String getCounterpart() {
		return mCounterpart;
	}

	@Override
	public boolean equals(Object o) {
		boolean equal = false;
		if (o instanceof ChatInformation) {
			ChatInformation chatInfo = (ChatInformation) o;
			if (chatInfo.getType() == mType) {
				if (chatInfo.getCounterpart().equals(mCounterpart)) {
					equal = true;
				}
			}
		}
		return equal;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mCounterpart);
		dest.writeInt(mType);
	}

	public static final Parcelable.Creator<ChatInformation> CREATOR = new Parcelable.Creator<ChatInformation>() {
		public ChatInformation createFromParcel(Parcel in) {
			return new ChatInformation(in);
		}

		public ChatInformation[] newArray(int size) {
			return new ChatInformation[size];
		}
	};

	private ChatInformation(Parcel in) {
		this(in.readInt(), in.readString());
	}

	public String getUsername() {
		return StringUtils.parseName(mCounterpart);
	}
}
