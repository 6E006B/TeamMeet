package de.teammeet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class FormTeamDialog extends DialogFragment {
	private static final String CLASS = FormTeamDialog.class.getSimpleName();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		final LayoutInflater factory = LayoutInflater.from(getActivity());
		final View formTeamView = factory.inflate(R.layout.form_team_dialog, null);
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.form_team_dialog_title);
		builder.setView(formTeamView);
		builder.setPositiveButton(R.string.button_create, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				EditText teamNameView = (EditText) formTeamView.findViewById(R.id.form_team_dialog_teamname);
				String teamName = teamNameView.getText().toString();
				Log.d(CLASS, String.format("chosen team name: %s", teamName));
				ContactsFragment contacts = (ContactsFragment) getTargetFragment();
				contacts.enteredTeamName(teamName);
			}
		});
		return builder.create();
    }
}
