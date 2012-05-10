package de.teammeet.services.xmpp;

import java.io.IOException;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;


public class TeamMeetExtensionProvider implements PacketExtensionProvider {

	private final static String CLASS = TeamMeetExtensionProvider.class.getSimpleName();

	@Override
	public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
		TeamMeetPacketExtension teamMeetPacket = null;
		MatePacket matePacket = null;
		IndicatorPacket indicatorPacket = null;

		// iterate over all XML tags
		boolean done = false;
		while (!done) {
			int eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG) {
				if (parser.getName().equals(TeamMeetPacketExtension.TEAMMEET)) {
					Log.d(CLASS, "Found teammeet tag");
				} else if (parser.getName().equals(TeamMeetPacketExtension.TEAMMEET)) {
					matePacket = parseMatePacketExtension(parser);
				} else if (parser.getName().equals(TeamMeetPacketExtension.INDICATOR)) {
					indicatorPacket = parseIndicatorPacketExtension(parser);
				} else {
					throw new InvalidProtocolException(String.format("Found invalid opening tag '%s'", parser.getName())); 
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				// TODO: What happens if we never meet a </teammet> tag and the parser runs out of tags?
				if (parser.getName().equals(TeamMeetPacketExtension.TEAMMEET)) {
					done = true;
				}
			}
		}

		if (matePacket != null || indicatorPacket != null) {
			teamMeetPacket = new TeamMeetPacketExtension(matePacket, indicatorPacket);
		} else {
			throw new InvalidProtocolException("Missing value in TeamMeet XML message");
		}

		return teamMeetPacket;
	}

	private MatePacket parseMatePacketExtension(XmlPullParser parser)
			throws InvalidProtocolException, XmlPullParserException, IOException {
		MatePacket matePacket = null;
		int lon = 0;
		int lat = 0;
		int accuracy = 0;
		boolean done = false;
		while (!done) {
			int eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG) {
				if (parser.getName().equals(TeamMeetPacketExtension.LON)) {
					lon = Integer.parseInt(parser.nextText());
				} else if (parser.getName().equals(TeamMeetPacketExtension.LAT)) {
					lat = Integer.parseInt(parser.nextText());
				} else if (parser.getName().equals(TeamMeetPacketExtension.ACCURACY)) {
					accuracy = Integer.parseInt(parser.nextText());
				} else {
					throw new InvalidProtocolException(String.format("Found invalid opening tag '%s'", parser.getName())); 
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				// TODO: What happens if we never meet a </mate> tag and the parser runs out of tags?
				if (parser.getName().equals(TeamMeetPacketExtension.MATE)) {
					done = true;
				}
			}
		}

		// check we've got everything we need
		if (lon != 0 &&
		    lat != 0 &&
		    accuracy != 0) {
			matePacket = new MatePacket(lon, lat, accuracy);
		} else {
			throw new InvalidProtocolException("Missing value in Mate XML message");
		}
		return matePacket;
	}

	private IndicatorPacket parseIndicatorPacketExtension(XmlPullParser parser)
			throws InvalidProtocolException, XmlPullParserException, IOException {
		IndicatorPacket indicatorPacket = null;
		int lon = 0;
		int lat = 0;
		String info = null;

		// iterate over all XML tags
		boolean done = false;
		while (!done) {
			int eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG) {
				if (parser.getName().equals(TeamMeetPacketExtension.LON)) {
					lon = Integer.parseInt(parser.nextText());
				} else if (parser.getName().equals(TeamMeetPacketExtension.LAT)) {
					lat = Integer.parseInt(parser.nextText());
				} else if (parser.getName().equals(TeamMeetPacketExtension.INFO)) {
					info = parser.nextText();
				} else {
					throw new InvalidProtocolException(String.format("Found invalid opening tag '%s'", parser.getName())); 
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				// TODO: What happens if we never meet a </indicator> tag and the parser runs out of tags?
				if (parser.getName().equals(TeamMeetPacketExtension.INDICATOR)) {
					done = true;
				}
			}

		}

		// check we've got everything we need
		if (lon != 0 && lat != 0 && info != null) {
			indicatorPacket = new IndicatorPacket(lon, lat, info);
		} else {
			throw new InvalidProtocolException("Missing value in Indicator message");
		}
		return indicatorPacket;
	}

	public static class InvalidProtocolException extends XMPPException {
		private static final long serialVersionUID = 3956025292889427895L;
		
		public InvalidProtocolException(String reason) {
			super(reason);
		}
	}

}