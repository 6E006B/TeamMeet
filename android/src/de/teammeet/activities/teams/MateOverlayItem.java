package de.teammeet.activities.teams;

import org.jivesoftware.smack.util.StringUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

import de.teammeet.R;

public class MateOverlayItem extends OverlayItem {

	private Mate mMate = null;
	private Context mContext = null;

	public MateOverlayItem(Mate mate, Context context) {
		super(mate.getLocation(), mate.getID(), mate.getID());
		mMate  = mate;
		mContext = context;
		Drawable drawable = createDrawable();
		setMarker(drawable);
	}

	@Override
	public GeoPoint getPoint() {
		return mMate.getLocation();
	}

	public Mate getMate() {
		return mMate;
	}

	private Drawable createDrawable() {
		int textSize = mContext.getResources().getDimensionPixelSize(R.dimen.mate_icon_text_size);
		Paint innerPaint = createInnerTextPaint(textSize);
		Paint outerPaint = createOuterTextPaint(textSize);

		String username = StringUtils.parseResource(mMate.getID());

		Bitmap referenceBitmap = BitmapFactory.decodeResource(mContext.getResources(),
		                                                  R.drawable.ic_map_mate);
		Rect textBounds = calculateTextBounds(outerPaint, username);
		int width = referenceBitmap.getWidth() + textBounds.width();
		int height = referenceBitmap.getHeight() + textBounds.height();
		// create a mutable bitmap with the needed size for the icon and text
		Bitmap workingBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(workingBitmap);
		canvas.drawBitmap(referenceBitmap, 0, textBounds.height(), null);

		float textXOffset = referenceBitmap.getWidth() + textBounds.exactCenterX();
		float textYOffset = textBounds.height();
		canvas.drawText(username, textXOffset, textYOffset, outerPaint);
		canvas.drawText(username, textXOffset, textYOffset, innerPaint);
		canvas.save();
		Drawable drawable = new BitmapDrawable(mContext.getResources(), workingBitmap);
		//XXX: watch out - magic ahead ...
		// For some reason the image gets placed at the (0,0) coordinate, so by moving it to the
		// upper left so much, that the center of the image is at (0,0) will result in the center
		// of the ball being at the exact mates position.
		drawable.setBounds(referenceBitmap.getWidth()/-2,
		                   (textBounds.height() + referenceBitmap.getHeight()/2) * -1,
		                   referenceBitmap.getWidth()/2 + textBounds.width(),
		                   referenceBitmap.getHeight()/2);
		return drawable;
	}

	private static Paint createInnerTextPaint(float textSize) {
		Paint innerPaint = new Paint();
		innerPaint.setColor(Color.BLACK);
		innerPaint.setTextSize(textSize);
		innerPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		innerPaint.setTypeface(Typeface.DEFAULT);
		innerPaint.setTextAlign(Paint.Align.CENTER);
		return innerPaint;
	}

	private static Paint createOuterTextPaint(float textSize) {
		Paint outerPaint = new Paint();
		outerPaint.setARGB(196, 255, 255, 255);
		outerPaint.setTextSize(textSize);
		outerPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		outerPaint.setTypeface(Typeface.DEFAULT);
		outerPaint.setTextAlign(Paint.Align.CENTER);
		outerPaint.setStyle(Paint.Style.STROKE);
		outerPaint.setStrokeWidth(3);
		return outerPaint;
	}

	private static Rect calculateTextBounds(Paint paint, String text) {
		Rect textBounds = new Rect();
		paint.getTextBounds(text, 0, text.length(), textBounds);
		return textBounds;
	}
}
