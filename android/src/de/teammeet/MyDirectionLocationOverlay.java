package de.teammeet;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class MyDirectionLocationOverlay extends MyLocationOverlay {

	private MapView mMapView = null;
	private Paint mPaintPlayer = null;
	private double mArrowLength = 0;
	
	public MyDirectionLocationOverlay(Context context, MapView mapView) {
		super(context, mapView);
		mMapView = mapView;
		Resources resources = mapView.getResources();
		mPaintPlayer = new Paint();
		mPaintPlayer.setStyle(Paint.Style.FILL);
		mPaintPlayer.setColor(resources.getColor(R.color.paint_default));
		mArrowLength = resources.getInteger(R.integer.arrow_length);
	}

	@Override
	protected void drawCompass(Canvas canvas, float bearing) {
		GeoPoint position = getMyLocation();
		
		if (position != null) {
			final Point coords = new Point();
			mMapView.getProjection().toPixels(position, coords);
			final Path path = new Path();
			path.moveTo((float) (coords.x + mArrowLength *
							Math.sin((-bearing + 180) * Math.PI / 180)),
						(float) (coords.y + mArrowLength *
							Math.cos((-bearing + 180) * Math.PI / 180)));
			path.lineTo((float) (coords.x + (mArrowLength / 4) *
							Math.sin((-bearing + 90) * Math.PI / 180)),
						(float) (coords.y + (mArrowLength / 4) *
							Math.cos((-bearing + 90) * Math.PI / 180)));
			path.lineTo((float) (coords.x + (mArrowLength / 4) *
							Math.sin((-bearing - 90) * Math.PI / 180)),
						(float) (coords.y + (mArrowLength / 4) *
							Math.cos((-bearing - 90) * Math.PI / 180)));
			path.close();
			canvas.drawPath(path, mPaintPlayer);
		}
	}
}
