package de.teammeet.services.xmpp;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHParameterSpec;

import org.jivesoftware.smack.util.Base64;

import android.util.Log;

public class KeyExchangePartner {

	private static final String CLASS = KeyExchangePartner.class.getSimpleName();

	private static final BigInteger mDHPrime = new BigInteger("145135528544641048828781082741133792265510283642227563843882120490266943454709965696885526964860090920378220705587684343691655547422645061028417617789225411492181871509076266136947361308537150838075923208869454733438857938120530479235883529669535849982509957532677257289115470567900093536536896831965040387969");
	private static final BigInteger mDHGenerator = new BigInteger("88291786162459212881905949060861099478890630921833730602301026928256212377008840401387215755929281334909231489875975120117299561515866296992376827924649080932422589386576501108138166912979093438394542997153702864014749319348771381994641762187325694782184297315316416077796892478055266319844543738559065819115");
	private static final int mDHExponentSize = 1023;

	private final String mName;
	private KeyPair mKeyPair;
	private byte[] mSharedSecret;

	public KeyExchangePartner(String name) {
		mName = name;
		mKeyPair = generateKeyPair();
		mSharedSecret = null;
	}

	private KeyPair generateKeyPair() {
		KeyPair keyPair = null;
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
			keyPairGenerator.initialize(new DHParameterSpec(mDHPrime, mDHGenerator, mDHExponentSize));

			keyPair = keyPairGenerator.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			//TODO: Inform user via UI
			Log.e(CLASS, String.format("Could not acquire key pair generator to exchange session key with '%s': %s", mName, e.getMessage()));
		} catch (InvalidAlgorithmParameterException e) {
			//TODO: Inform user via UI
			Log.e(CLASS, String.format("Could not initialize key pair generator to exchange session key with '%s': %s", mName, e.getMessage()));
		}
		return keyPair;
	}

	public String getName() {
		return mName;
	}

	public byte[] getPublicKey() {
		return mKeyPair.getPublic().getEncoded();
	}

	public void calculateSharedSecret(byte[] publicKeyBytes) {
		Log.d(CLASS, String.format("Generating shared secret for '%s'", mName));

		try {
			PrivateKey ownPrivateKey = mKeyPair.getPrivate();
			PublicKey foreignPublicKey = restoreKey(publicKeyBytes);
			KeyAgreement keyAgreement = setupKeyAgreement(ownPrivateKey, foreignPublicKey);

			mSharedSecret = keyAgreement.generateSecret();
			Log.d(CLASS, String.format("Generated shared secret: %s", Base64.encodeBytes(mSharedSecret)));

		} catch (InvalidKeyException e) {
			//TODO: Notify user via UI
			Log.e(CLASS, String.format("Key used to agree upon secret is invalid: %s",
										e.getMessage()));
		}
	}

	private PublicKey restoreKey(byte[] keyBytes) {
		X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
		PublicKey publicKey = null;
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("DH");
			publicKey = keyFactory.generatePublic(x509KeySpec);
		} catch (NoSuchAlgorithmException e) {
			//TODO: Notify user via UI
			Log.e(CLASS, String.format("Could not get Diffie-Hellman key factory: %s",
										e.getMessage()));
		} catch (InvalidKeySpecException e) {
			//TODO: Notify user via UI
			Log.e(CLASS, String.format("Key spec used to decrypt received public key is invalid: %s",
										e.getMessage()));
		}
		return publicKey;
	}

	private KeyAgreement setupKeyAgreement(PrivateKey privKey, PublicKey pubKey) throws InvalidKeyException {
		KeyAgreement keyAgreement = null;
		try {
			keyAgreement = KeyAgreement.getInstance("DH");
			keyAgreement.init(privKey);
			keyAgreement.doPhase(pubKey, true);
		} catch (NoSuchAlgorithmException e) {
			//TODO: Notify user via UI
			Log.e(CLASS, String.format("Could not fetch Diffie-Hellman key agreement: %s",
										e.getMessage()));
		}
		return keyAgreement;
	}

	@Override
	public String toString() {
		return mName;
	}
}
