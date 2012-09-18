package de.teammeet.activities.roster;

import java.util.List;

import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.teammeet.R;
import de.teammeet.activities.roster.ContactsFragment.ContactlistChild;

public class ContactlistAdapter extends BaseExpandableListAdapter {

	private static final String CLASS = ContactsFragment.class.getSimpleName();

	private FragmentActivity mActivity;
	private List<String> mGroups;
	private List<List<ContactlistChild>> mChildren;


	public ContactlistAdapter(FragmentActivity activity,
							  List<String> groups,
							  List<List<ContactlistChild>> children
							 ) {
		super();

		mActivity = activity;
		mChildren = children;
		mGroups = groups;
	}

	@Override
	public ContactlistChild getChild(int groupPosition, int childPosition) {
		return mChildren.get(groupPosition).get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return groupPosition * 1024 + childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		if (convertView == null || !(convertView instanceof TextView)) {
			TextView contactName;
			TextView contactStatusMessage;
			ImageView contactStatusImage;

			convertView = mActivity.getLayoutInflater().inflate(R.layout.contactlist_child, null, false);

			// Populate your custom view here
			ContactlistChild child = (ContactlistChild) getChild(groupPosition, childPosition);

			contactName = (TextView)convertView.findViewById(R.id.contact_name);
			contactName.setText((String) child.mName);

			contactStatusImage = (ImageView) convertView.findViewById(R.id.contact_status_image);
			contactStatusImage.setImageResource((int) child.mMode);

			contactStatusMessage = (TextView)convertView.findViewById(R.id.contact_status_message);
			contactStatusMessage.setText((String) child.mStatus);
		}
		return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return mChildren.get(groupPosition).size();
	}

	@Override
	public String getGroup(int groupPosition) {
		return mGroups.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return mGroups.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
			ViewGroup parent) {
		if (convertView == null || !(convertView instanceof TextView)) {
			convertView = mActivity.getLayoutInflater().inflate(android.R.layout.simple_expandable_list_item_1, parent, false);

			TextView groupName = (TextView)convertView.findViewById(android.R.id.text1);
			groupName.setText(mGroups.get(groupPosition));
		}
		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
}
