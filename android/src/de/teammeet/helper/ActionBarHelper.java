package de.teammeet.helper;

import android.app.Activity;
import android.content.Intent;
import de.teammeet.activities.roster.RosterActivity;

public class ActionBarHelper {
	public static void navigateUpInHierarchy(Activity activity) {
		Intent intent = new Intent(activity, RosterActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activity.startActivity(intent);
	}
}
