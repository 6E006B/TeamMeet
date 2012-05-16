package de.teammeet.services.xmpp;

import java.security.KeyPair;

public class Invitee {

	private final String mName;
	private KeyPair mKeyPair = null;

	public Invitee(String name) {
		mName = name;
	}

	public String getName() {
		return mName;
	}

	public KeyPair getKeyPair() {
		return mKeyPair;
	}

	public void setKeyPair(KeyPair keypair) {
		mKeyPair = keypair;
	}

	public boolean hasBeenContacted() {
		return mKeyPair != null;
	}
}
