package de.teammeet.activities.roster;

import de.teammeet.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class Teams extends Fragment {

	private static final String CLASS = Teams.class.getSimpleName();
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return (LinearLayout) inflater.inflate(R.layout.teams, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(CLASS, "Resuming teams fragment");
	}

	@Override
	public void onPause() {
		Log.d(CLASS, "Pausing teams fragement");
		super.onPause();
	}

}
