package de.teammeet.activities.roster;

import org.jivesoftware.smack.util.StringUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import de.teammeet.R;

public class JoinTeamDialog extends DialogFragment {
	
	private final String mTeam;
	private final String mInviter;
	private final String mReason;
	private final String mPassword;

	public JoinTeamDialog(String team, String inviter, String reason, String password) {
		super();
		mTeam = team;
		mInviter = inviter;
		mReason = reason;
		mPassword = password;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final RosterActivity roster = (RosterActivity) getActivity();
		
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.join_team_dialog_title);
		builder.setCancelable(false);
		builder.setMessage(String.format("%s wants you to join '%s':\n%s",
				 StringUtils.parseName(mInviter),
				 StringUtils.parseName(mTeam),
				 mReason)
				);
		builder.setPositiveButton(R.string.button_join, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					final SharedPreferences settings = PreferenceManager.
							getDefaultSharedPreferences(roster);
					final String userIDKey = getString(R.string.preference_user_id_key);
					final String userID = settings.getString(userIDKey, "anonymous");
					
					roster.clickedJoinTeam(mTeam, userID, mPassword, mInviter);
			   }
		   });
		builder.setNegativeButton(R.string.button_decline, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				roster.clickedRejectTeam(mTeam, mInviter);
			}
		});
		return builder.create();
	}
}