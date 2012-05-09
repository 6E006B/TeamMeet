package de.teammeet.services.xmpp;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

import android.util.Log;


public class GeolocExtensionProvider implements PacketExtensionProvider {

	private final static String CLASS = GeolocExtensionProvider.class.getSimpleName();

	@Override
	public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
		GeolocPacketExtension geoloc = null;
		int lon = 0;
		int lat = 0;
		int accuracy = 0;

		// iterate over all XML tags
		boolean done = false;
		while (!done) {
			int eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG) {
				if (parser.getName().equals(GeolocPacketExtension.GEOLOC)) {
					Log.d(CLASS, "Found geoloc tag");
				} else if (parser.getName().equals(GeolocPacketExtension.LON)) {
					lon = Integer.parseInt(parser.nextText());
				} else if (parser.getName().equals(GeolocPacketExtension.LAT)) {
					lat = Integer.parseInt(parser.nextText());
				} else if (parser.getName().equals(GeolocPacketExtension.ACCURACY)) {
					accuracy = Integer.parseInt(parser.nextText());
				} else {
					throw new InvalidProtocolException(String.format("Found invalid opening tag '%s'", parser.getName())); 
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				// TODO: What happens if we never meet a </geoloc> tag and the parser runs out of tags?
				if (parser.getName().equals(GeolocPacketExtension.GEOLOC)) {
					done = true;
				}
			}

		}

		// check we've got everything we need
		if (lon != 0 &&
		    lat != 0 &&
		    accuracy != 0) {
			geoloc = new GeolocPacketExtension(lon, lat, accuracy);
		} else {
			throw new InvalidProtocolException("Missing value in Geoloc message");
		}
		
		return geoloc;

	}
	
	public static class InvalidProtocolException extends XMPPException {
		private static final long serialVersionUID = 3956025292889427895L;
		
		public InvalidProtocolException(String reason) {
			super(reason);
		}
	}

}
