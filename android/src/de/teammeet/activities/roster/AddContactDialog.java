package de.teammeet.activities.roster;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import de.teammeet.R;
import de.teammeet.interfaces.IXMPPService;

public class AddContactDialog extends DialogFragment {
	private static final String CLASS = AddContactDialog.class.getSimpleName();

	private IXMPPService mXMPPService = null;
	private String mGroup = null;

	public AddContactDialog(IXMPPService xmppService) {
		super();
		mXMPPService = xmppService;
	}

	public AddContactDialog(IXMPPService xmppService, String group) {
		this(xmppService);
		mGroup = group;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final LayoutInflater factory = LayoutInflater.from(getActivity());
		final View addContactView = factory.inflate(R.layout.add_contact_dialog, null);
		final String[] groups = mXMPPService.getGroups();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_dropdown_item_1line, groups);
		AutoCompleteTextView textView = (AutoCompleteTextView) addContactView.
				findViewById(R.id.add_contact_dialog_groupname);
		textView.setAdapter(adapter);
		if (mGroup != null) {
			textView.setText(mGroup);
		}

		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.add_contact_dialog_title);
		builder.setView(addContactView);
		builder.setPositiveButton(R.string.button_add, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				EditText contactNameView = (EditText) addContactView.findViewById(R.id.add_contact_dialog_contactname);
				EditText groupNameView = (EditText) addContactView.findViewById(R.id.add_contact_dialog_groupname);
				String contactName = contactNameView.getText().toString();
				String groupName = groupNameView.getText().toString();
				Log.d(CLASS, String.format("add contact '%s' to '%s'", contactName, groupName));
				((RosterActivity) getActivity()).addContact(contactName, groupName);
			}
		});
		return builder.create();
	}
}
