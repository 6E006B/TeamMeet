package de.teammeet.services.xmpp;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

import android.util.Log;


public class IndicatorExtensionProvider implements PacketExtensionProvider {

	private final static String CLASS = IndicatorExtensionProvider.class.getSimpleName();

	@Override
	public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
		IndicatorPacketExtension indicator = null;
		int lon = 0;
		int lat = 0;
		String info = null;

		// iterate over all XML tags
		boolean done = false;
		while (!done) {
			int eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG) {
				if (parser.getName().equals(IndicatorPacketExtension.INDICATOR)) {
					Log.d(CLASS, "Found geoloc tag");
				} else if (parser.getName().equals(IndicatorPacketExtension.LON)) {
					lon = Integer.parseInt(parser.nextText());
				} else if (parser.getName().equals(IndicatorPacketExtension.LAT)) {
					lat = Integer.parseInt(parser.nextText());
				} else if (parser.getName().equals(IndicatorPacketExtension.INFO)) {
					info = parser.nextText();
				} else {
					throw new InvalidProtocolException(String.format("Found invalid opening tag '%s'", parser.getName())); 
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				// TODO: What happens if we never meet a </indicator> tag and the parser runs out of tags?
				if (parser.getName().equals(IndicatorPacketExtension.INDICATOR)) {
					done = true;
				}
			}

		}

		// check we've got everything we need
		if (lon != 0 && lat != 0 && info != null) {
			indicator = new IndicatorPacketExtension(lon, lat, info);
		} else {
			throw new InvalidProtocolException("Missing value in Indicator message");
		}
		
		return indicator;

	}
	
	public static class InvalidProtocolException extends XMPPException {
		private static final long serialVersionUID = 3956025292889427895L;
		
		public InvalidProtocolException(String reason) {
			super(reason);
		}
	}

}
