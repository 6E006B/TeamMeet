package de.teammeet.services.xmpp;

import org.jivesoftware.smack.util.Base64;

public class CryptoPacket {

	private String mType;
	private byte[] mKey;
	private String mTeam;

	public CryptoPacket(final String TYPE, byte[] key, String team) {
		mType = TYPE;
		mKey = key;
		mTeam = team;
	}

	public boolean isPublicKey() {
		return mType.equals(TeamMeetPacketExtension.KEYTYPE_PUBLIC);
	}

	public boolean isSharedSecret() {
		return mType.equals(TeamMeetPacketExtension.KEYTYPE_SECRET);
	}

	public byte[] getKey() {
		return mKey;
	}

	public String getTeam() {
		return mTeam;
	}

	public String toXML() {
		return String.format("<%s>" +
				             "<%s %s=\"%s\" >" +
				             "%s" +
				             "</%s>" +
				             "<%s>" +
				             "%s" +
				             "</%s>" +
						     "</%s>",
				             TeamMeetPacketExtension.CRYPTO,
				             TeamMeetPacketExtension.CRYPTO_KEY, TeamMeetPacketExtension.KEYTYPE_ATTR, mType,
				             Base64.encodeBytes(mKey),
				             TeamMeetPacketExtension.CRYPTO_KEY,
				             TeamMeetPacketExtension.TEAM,
				             mTeam,
				             TeamMeetPacketExtension.TEAM,
				             TeamMeetPacketExtension.CRYPTO);
	}
}
