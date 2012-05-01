package de.teammeet;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class MateOverlayItem extends OverlayItem {

	private Mate mMate = null;

	public MateOverlayItem(Mate mate) {
		super(mate.getLocation(), mate.getID(), mate.getID());
		mMate  = mate;
	}

	@Override
	public GeoPoint getPoint() {
		return mMate.getLocation();
	}

	public Mate getMate() {
		return mMate;
	}
}
