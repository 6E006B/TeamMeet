package de.teammeet.services.xmpp;

import org.jivesoftware.smack.packet.PacketExtension;

import android.util.Log;

public class TeamMeetPacketExtension implements PacketExtension {

	private static final String CLASS = TeamMeetPacketExtension.class.getSimpleName();

	public static final String NAMESPACE = "https://teammeet.de/teammeet.ns";
	public static final String TEAMMEET = "teammeet";
	public static final String MATE = "mate";
	public static final String INDICATOR = "indicator";
	public static final String LON = "lon";
	public static final String LAT = "lat";
	public static final String ACCURACY = "err";
	public static final String INFO = "info";

	private MatePacket mMatePacket;
	private IndicatorPacket mIndicatorPacket;

	public TeamMeetPacketExtension(MatePacket matePacket, IndicatorPacket indicatorPacket) {
		mMatePacket = matePacket;
		mIndicatorPacket = indicatorPacket;
	}

	@Override
	public String getElementName() {
		return TEAMMEET;
	}

	@Override
	public String getNamespace() {
		Log.d(CLASS, "getNamespace() called");
		return NAMESPACE;
	}

	@Override
	public String toXML() {
		String mateXML = "";
		String indicatorXML = "";
		if (mMatePacket != null) {
			mateXML = mMatePacket.toXML();
		}
		if (mIndicatorPacket != null) {
			indicatorXML = mIndicatorPacket.toXML();
		}
		return String.format("<x xmlns=\"%s\"><%s>%s%s</%s></x>",
				             getNamespace(), TEAMMEET, mateXML, indicatorXML, TEAMMEET);
	}
	
	public boolean hasMatePacket() {
		return mMatePacket != null;
	}

	public boolean hasIndicatorPacket() {
		return mIndicatorPacket != null;
	}

	public MatePacket getMatePacket() {
		return mMatePacket;
	}

	public IndicatorPacket getIndicatorPacket() {
		return mIndicatorPacket;
	}

}
