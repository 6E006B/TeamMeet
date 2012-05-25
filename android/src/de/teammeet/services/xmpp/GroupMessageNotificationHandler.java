package de.teammeet.services.xmpp;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import de.teammeet.activities.chat.Chat;
import de.teammeet.activities.chat.ChatsActivity;

public class GroupMessageNotificationHandler extends NotificationHandler {

	protected static String CLASS = GroupMessageNotificationHandler.class.getSimpleName();
	protected static final String CONTENT_TITLE = "Group chat message received";

	public GroupMessageNotificationHandler(Context context, int icon, int notificationID) {
		super(context, icon, notificationID);
	}

	public void newNotification(Bundle bundle) {
		final PendingIntent contentIntent = createPendingIntent(ChatsActivity.class, bundle);

		final CharSequence notificationText = createNotificationText(bundle);
		final String tickerText = String.format("New team message in %s", notificationText);
		final Notification notification = buildNotificaton(CONTENT_TITLE, notificationText,
		                                                   tickerText, contentIntent);
		notify(notification);
	}

	
	/**
	 * Helper function to create the desired Bundle for the GroupMessageNotificationHandler.
	 * 
	 * @param room The room the invitation points to.
	 * @param inviter The contact that sent the invitation.
	 * @param reason The given reason for the invitation.
	 * @param password The password for the room, that is needed to join.
	 * @return A ready to use Bundle for newNotification().
	 */
	public static Bundle generateBundle(String sender, String from, String message) {
		Bundle bundle = new Bundle();
		bundle.putInt(XMPPService.TYPE, Chat.TYPE_GROUP_CHAT);
		bundle.putString(XMPPService.SENDER, sender);
		bundle.putString(XMPPService.FROM, from);
		bundle.putString(XMPPService.MESSAGE, message);
		return bundle;
	}

	protected String createNotificationText(Bundle bundle) {
		final String from = bundle.getString(XMPPService.FROM, "");
		final String sender = bundle.getString(XMPPService.SENDER, "");
		final String message = bundle.getString(XMPPService.MESSAGE, "");
		return String.format("%s (%s) : %s", from, sender, message);
	}
}
