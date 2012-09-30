package de.teammeet.activities.chat;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import de.teammeet.R;
import de.teammeet.activities.chat.Chat.ChatEntry;

public class ChatAdapter extends ArrayAdapter<ChatEntry> {

	private Context mContext;
	private Resources mResources;

	public ChatAdapter(Context context, int chatItem) {
		super(context, chatItem);
		mContext = context;
		mResources = context.getResources();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		if (convertView == null || !(convertView instanceof TextView)) {
			// Inflate new view
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.chat_item, parent, false);

			// Initialize view with padding and foreground colour
			convertView.setPadding(2, 2, 2, 2);
			((TextView)convertView).setTextColor(mResources.getColor(R.color.chatText));
		}

		// Update text
		ChatEntry entry = (ChatEntry)getItem(position);
		((TextView)convertView).setText(entry.format());

		// Update background colour
		int backgroundID = entry.isFromMe()? R.color.chatMyBackground : R.color.chatOtherBackground;
		convertView.setBackgroundColor(mResources.getColor(backgroundID));

		return convertView;
	}

}
