package de.teammeet.activities.teams;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import de.teammeet.R;
import de.teammeet.R.color;
import de.teammeet.R.integer;

public class MapGestureDetectorOverlay extends Overlay implements OnGestureListener {

	private static final String CLASS = MapGestureDetectorOverlay.class.getSimpleName();
	private GestureDetector gestureDetector;
	private OnGestureListener onGestureListener;
	private MapView mMapView = null;
	private GeoPoint mIndicatorLocation = null;

	private final ReentrantLock	mLock					= new ReentrantLock();

	private List<String>		mAddressOfPoint			= null;
	private Resources			mResources				= null;
	private int					mIndicatorSize			= 0;
	private int					mIndicatorWidth			= 0;
	private Paint				mPaintIndicator			= null;
	private Paint				mPaintIndicatorText		= null;
	
	public MapGestureDetectorOverlay(MapView mapView, Resources resources) {
		gestureDetector = new GestureDetector(this);
		mMapView = mapView;
		
		mResources = resources;
		mIndicatorSize = mResources.getInteger(R.integer.indicator_size);
		mIndicatorWidth = mResources.getInteger(R.integer.indicator_width);

		mPaintIndicator = new Paint();
		mPaintIndicator.setColor(mResources.getColor(R.color.paint_indicator));
		mPaintIndicator.setStrokeWidth(mIndicatorWidth);
		mPaintIndicator.setStyle(Paint.Style.STROKE);

		mPaintIndicatorText = new Paint();
		mPaintIndicatorText.setColor(mResources.getColor(R.color.paint_indicator_text));
		mPaintIndicatorText.setAntiAlias(true);
		mPaintIndicatorText.setTextSize(mResources.getInteger(R.integer.indicator_text_size));
		mPaintIndicatorText.setTypeface(Typeface.DEFAULT_BOLD);
		mPaintIndicatorText.setShadowLayer(mResources.getInteger(R.integer.indicator_text_border_width), 1,
				1, mResources.getColor(R.color.paint_indicator_text_border));
	}

	public MapGestureDetectorOverlay(MapView mapView, Resources resources,
			OnGestureListener onGestureListener) {
		this(mapView, resources);
		setOnGestureListener(onGestureListener);
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
		float x = event.getX();
		float y = event.getY();
		Log.d(CLASS, String.format("x: %f y: %f", x, y));
		Log.d(CLASS, String.format("x: %d y: %d", (int)x, (int)y));
		mIndicatorLocation  = mMapView.getProjection().fromPixels((int)x, (int)y);
		Geocoder geoCoder = new Geocoder(mMapView.getContext(), Locale.getDefault());
		try {
			List<Address> addresses = geoCoder.getFromLocation(mIndicatorLocation.getLatitudeE6() / 1E6,
					mIndicatorLocation.getLongitudeE6() / 1E6, 1);

			List<String> addressStringList = new LinkedList<String>();
			if (addresses.size() > 0) {
				for (int i = 0; i < addresses.get(0).getMaxAddressLineIndex(); i++)
					addressStringList.add(addresses.get(0).getAddressLine(i));
			}
			// Collections.reverse(addressStringList);
			mAddressOfPoint = addressStringList;
		} catch (IOException e) {
			Log.e(CLASS, "Error using Geocoder: " + e.getLocalizedMessage());
			e.printStackTrace();
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
	
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
		if (mIndicatorLocation != null) {
			acquireLock();
			try {
				Projection projection = mapView.getProjection();
				final Point screenPoint = new Point();

				projection.toPixels(mIndicatorLocation, screenPoint);
				canvas.drawCircle(screenPoint.x, screenPoint.y, mIndicatorSize + 1, mPaintIndicator);

				if (mAddressOfPoint != null) {
					int x = screenPoint.x + mIndicatorSize + mIndicatorWidth;
					int y = screenPoint.y - mIndicatorSize / 2 - mIndicatorWidth / 2;
					for (String line : mAddressOfPoint) {
						canvas.drawText(line, x, y, mPaintIndicatorText);
						y -= mPaintIndicatorText.ascent() - mPaintIndicatorText.descent() * 2f;
					}
				}
			} finally {
				releaseLock();
			}
		}
	}
	

	private void acquireLock() {
		mLock.lock();
	}

	private void releaseLock() {
		mLock.unlock();
	}
}