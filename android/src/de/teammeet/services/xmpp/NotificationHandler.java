package de.teammeet.services.xmpp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

/**
 *	Base class for handling the creation of new Notifications.
 */
public abstract class NotificationHandler {

	protected Context mContext;
	protected int mIcon;
	protected int mNotificationID;
	protected NotificationCompat.Builder mNotificationBuilder;
	protected NotificationManager mNotificationManager;

	
	/**
	 * @param context The Context from which the notification is created.
	 * @param icon The resource of the icon to display in the notification bar.
	 * @param notificationID Identifier for this type of notifications.
	 */
	public NotificationHandler(Context context, int icon, int notificationID) {
		mContext = context;
		mIcon = icon;
		mNotificationID = notificationID;

		final String ns = Context.NOTIFICATION_SERVICE;
		mNotificationManager = (NotificationManager) mContext.getSystemService(ns);

		mNotificationBuilder = new NotificationCompat.Builder(mContext);
		mNotificationBuilder.setSmallIcon(mIcon);
		mNotificationBuilder.setAutoCancel(true);
		mNotificationBuilder.setDefaults(Notification.DEFAULT_ALL);
	}

	/**
	 * This method is to be implemented by the extending class.
	 * A typical structure of this method looks like the following:
	 * 
	 * 1. Use createPendingIntent() to create the content intent for the notification.
	 * 2. Call buildNotification() with the appropriate text elements and intent.
	 * 3. Call notify() with the notification.
	 * 
	 * @param bundle The bundle contains information needed to create the Intent and/or information
	 *               to be displayed in the notification itself.
	 */
	abstract public void newNotification(Bundle bundle);
	
	/**
	 * @param cls The class to be raised by the Intent.
	 * @param bundle The extra information Bundle that is to be associated with the intent.
	 * @return The PendingIntent that can now be used for buildNotification().
	 */
	protected PendingIntent createPendingIntent(Class<?> cls, Bundle bundle) {
		final Intent intent = createIntent(cls, bundle);
		return PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private Intent createIntent(Class<?> cls, Bundle bundle) {
		final Intent notificationIntent = new Intent(mContext, cls);
		notificationIntent.putExtras(bundle);
		return notificationIntent;
	}

	/**
	 * @param title The title to be displayed in the notifications view.
	 * @param text The description to be displayed in the notifications view.
	 * @param tickerText The ticker text displayed in the notification bar when the notification is
	 *                   is raised.
	 * @param pendingIntent The content intent associated with this notification. The contained
	 *                      Intent will be broadcasted when the user clicks on the notification.
	 * @return The Notification that can now be passed to notify().
	 */
	protected Notification buildNotificaton(CharSequence title, CharSequence text,
			CharSequence tickerText, PendingIntent pendingIntent) {
		mNotificationBuilder.setContentTitle(title);
		mNotificationBuilder.setContentText(text);
		mNotificationBuilder.setTicker(tickerText);
		mNotificationBuilder.setContentIntent(pendingIntent);
		return mNotificationBuilder.getNotification();
	}
	
	protected void notify(Notification notification) {
		mNotificationManager.notify(mNotificationID, notification);
	}
}
