package de.teammeet.services.xmpp;

import org.jivesoftware.smack.util.Base64;

public class CryptoPacket {

	private String mType;
	private byte[] mKey;

	public CryptoPacket(final String TYPE, byte[] key) {
		mType = TYPE;
		mKey = key;
	}
	
	public boolean isPublicKey() {
		return mType == TeamMeetPacketExtension.KEYTYPE_PUBLIC;
	}
	
	public boolean isSharedSecret() {
		return mType == TeamMeetPacketExtension.KEYTYPE_SECRET;
	}
	
	public byte[] getKey() {
		return mKey;
	}
	
	public String toXML() {
		return String.format("<%s>" +
				             "<%s %s=\"%s\" >" +
				             "%s" +
				             "</%s>" +
						     "</%s>",
				             TeamMeetPacketExtension.CRYPTO,
				             TeamMeetPacketExtension.CRYPTO_KEY, TeamMeetPacketExtension.KEYTYPE_ATTR, mType,
				             Base64.encodeBytes(mKey),
				             TeamMeetPacketExtension.CRYPTO_KEY,
				             TeamMeetPacketExtension.CRYPTO);
	}
}
