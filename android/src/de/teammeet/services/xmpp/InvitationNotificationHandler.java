package de.teammeet.services.xmpp;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import de.teammeet.activities.roster.RosterActivity;

public class InvitationNotificationHandler extends NotificationHandler {

	protected static String CLASS = InvitationNotificationHandler.class.getSimpleName();
	protected static final String CONTENT_TITLE = "Group Invitation received";

	public InvitationNotificationHandler(Context context, int icon, int notificationID) {
		super(context, icon, notificationID);
	}

	public void newNotification(Bundle bundle) {
		final PendingIntent contentIntent = createPendingIntent(RosterActivity.class, bundle);

		final CharSequence tickerText = createTickerText(bundle);
		final Notification notification = buildNotificaton(CONTENT_TITLE, tickerText, tickerText,
		                                                   contentIntent);
		notify(notification);
	}

	
	/**
	 * Helper function to create the desired Bundle for the InviationNotificationHandler.
	 * 
	 * @param room The room the invitation points to.
	 * @param inviter The contact that sent the invitation.
	 * @param reason The given reason for the invitation.
	 * @param password The password for the room, that is needed to join.
	 * @return A ready to use Bundle for newNotification().
	 */
	public static Bundle generateBundle(String room, String inviter, String password) {
		Bundle bundle = new Bundle();
		bundle.putInt(XMPPService.TYPE, XMPPService.TYPE_JOIN);
		bundle.putString(XMPPService.ROOM, room);
		bundle.putString(XMPPService.INVITER, inviter);
		bundle.putString(XMPPService.PASSWORD, password);
		return bundle;
	}

	protected String createTickerText(Bundle bundle) {
		final String room = bundle.getString(XMPPService.ROOM);
		final String inviter = bundle.getString(XMPPService.INVITER);
		return String.format("Invitation to '%s' from %s", room, inviter);
	}
}
