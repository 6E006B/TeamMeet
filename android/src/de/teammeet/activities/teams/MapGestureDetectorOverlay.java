package de.teammeet.activities.teams;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.jivesoftware.smack.XMPPException;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import de.teammeet.services.xmpp.XMPPService;

public class MapGestureDetectorOverlay extends Overlay implements OnGestureListener {

	private static final String CLASS = MapGestureDetectorOverlay.class.getSimpleName();
	private GestureDetector gestureDetector;
	private OnGestureListener onGestureListener;
	private MapView mMapView = null;
	private XMPPService mXMPPService = null;

	public MapGestureDetectorOverlay(MapView mapView, Context context) {
		gestureDetector = new GestureDetector(context, this);
		mMapView = mapView;
	}

	public MapGestureDetectorOverlay(MapView mapView, Context context,
			OnGestureListener onGestureListener) {
		this(mapView, context);
		setOnGestureListener(onGestureListener);
	}

	public void setXMPPService(XMPPService xmppService) {
		mXMPPService = xmppService;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, MapView mapView) {
		if (gestureDetector.onTouchEvent(event)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		if (onGestureListener != null) {
			return onGestureListener.onDown(e);
		}
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (onGestureListener != null) {
			return onGestureListener.onFling(e1, e2, velocityX, velocityY);
		}
		return false;
	}

	@Override
	public void onLongPress(MotionEvent event) {
		Log.e(CLASS, "longpress!!!1 " + event.toString());
		if (mXMPPService != null) {
			float x = event.getX();
			float y = event.getY();
			Log.d(CLASS, String.format("x: %f y: %f", x, y));
			Log.d(CLASS, String.format("x: %d y: %d", (int)x, (int)y));
			GeoPoint indicatorLocation = mMapView.getProjection().fromPixels((int)x, (int)y);
			Geocoder geoCoder = new Geocoder(mMapView.getContext(), Locale.getDefault());
			try {
				List<Address> addresses =
						geoCoder.getFromLocation(indicatorLocation.getLatitudeE6() / 1E6,
						                         indicatorLocation.getLongitudeE6() / 1E6, 1);
	
				String addressString = "";
				if (addresses.size() > 0) {
					for (int i = 0; i < addresses.get(0).getMaxAddressLineIndex(); i++) {
						addressString += addresses.get(0).getAddressLine(i) + "\n";
					}
					addressString = addressString.substring(0, addressString.length()-1);
				}
				
				mXMPPService.sendIndicator(indicatorLocation, addressString);
			} catch (IOException e) {
				Log.e(CLASS, "Error using Geocoder: " + e.getLocalizedMessage());
				e.printStackTrace();
			} catch (XMPPException e) {
				Log.e(CLASS, "Error sending Indicator: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
		if (onGestureListener != null) {
			onGestureListener.onLongPress(event);
		}
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		if (onGestureListener != null) {
			onGestureListener.onScroll(e1, e2, distanceX, distanceY);
		}
		return false;
	}

	@Override
 	public void onShowPress(MotionEvent e) {
		if (onGestureListener != null) {
			onGestureListener.onShowPress(e);
		}
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		if (onGestureListener != null) {
			onGestureListener.onSingleTapUp(e);
		}
		return false;
	}

	public boolean isLongpressEnabled() {
		return gestureDetector.isLongpressEnabled();
	}

	public void setIsLongpressEnabled(boolean isLongpressEnabled) {
		gestureDetector.setIsLongpressEnabled(isLongpressEnabled);
	}

	public OnGestureListener getOnGestureListener() {
		return onGestureListener;
	}

	public void setOnGestureListener(OnGestureListener onGestureListener) {
		this.onGestureListener = onGestureListener;
	}
}