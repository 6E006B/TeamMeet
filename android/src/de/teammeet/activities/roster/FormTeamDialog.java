package de.teammeet.activities.roster;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import de.teammeet.R;

public class FormTeamDialog extends DialogFragment {
	private static final String CLASS = FormTeamDialog.class.getSimpleName();

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final AlertDialog dialog;
		final LayoutInflater factory = LayoutInflater.from(getActivity());
		final View formTeamView = factory.inflate(R.layout.form_team_dialog, null);
		final View nameField = formTeamView.findViewById(R.id.form_team_dialog_teamname);

		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.form_team_dialog_title);
		builder.setView(formTeamView);
		builder.setPositiveButton(R.string.button_create, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				EditText teamNameView = (EditText) formTeamView.findViewById(R.id.form_team_dialog_teamname);
				String teamName = teamNameView.getText().toString();
				Log.d(CLASS, String.format("chosen team name: %s", teamName));
				((RosterActivity) getActivity()).enteredTeamName(teamName);
			}
		});
		dialog = builder.create();

		showKeyboard(dialog, nameField);

		return dialog;
	}

	private void showKeyboard(final AlertDialog dialog, View focusField) {
		focusField.setOnFocusChangeListener(new View.OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
				}
			}
		});
	}
}
