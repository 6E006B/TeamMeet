package de.teammeet.activities.roster;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import de.teammeet.R;
import de.teammeet.interfaces.IXMPPService;

public class RegistrationDialog extends DialogFragment {
	private static final String CLASS = RegistrationDialog.class.getSimpleName();

	private IXMPPService mXMPPService = null;
	private String mUserName = null;
	private String mPassword = null;

	public RegistrationDialog(IXMPPService xmppService) {
		super();
		mXMPPService = xmppService;
	}

	public RegistrationDialog(IXMPPService xmppService, String userName, String password) {
		this(xmppService);
		mUserName = userName;
		mPassword = password;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final LayoutInflater factory = LayoutInflater.from(getActivity());
		final View registrationView = factory.inflate(R.layout.registration_dialog, null);
		restoreFields(registrationView);
		
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.registration_dialog_title);
		builder.setView(registrationView);
		builder.setPositiveButton(R.string.button_register, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				EditText userNameView = (EditText) registrationView.findViewById(R.id.registration_dialog_username);
				EditText passwordView = (EditText) registrationView.findViewById(R.id.registration_dialog_password);
				final String userName = userNameView.getText().toString();
				final String password = passwordView.getText().toString();
				final String server = getString(R.string.hardcoded_xmpp_server);
				Log.d(CLASS, String.format("registering '%s@%s'", userName, server));
				((RosterActivity) getActivity()).registerAccount("jabber.ccc.de", userName, password);
			}
		});
		return builder.create();
	}

	private void restoreFields(View view) {
		if (mUserName != null) {
			EditText userNameView = (EditText) view.findViewById(R.id.registration_dialog_username);
			userNameView.setText(mUserName);
		}
		if (mPassword != null) {
			EditText passwordView = (EditText) view.findViewById(R.id.registration_dialog_password);
			passwordView.setText(mPassword);
		}
	}
}
