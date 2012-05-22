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

	private static final BigInteger mDHPrime = new BigInteger("18272908765313506287602320759527657775520320692647798335490881029055204796769075788035058905348791772750675869560215163904279231594503148218248769924368622735317033880817588450520117896207122042398084136822056076440336955421132159625776083724490429596972687100185450228147409870497864510630373583293887114111648666720978632625600889233103253817997356028490589739969280373144812440793513978082933970540680259598563184018834247856478651984018347394108678026890591181808988152936049269250175868969388904508640188866554047063907322718511745201325574457362400571599750088494258859634399545572655808275552874044591139917879");
	private static final BigInteger mDHGenerator = new BigInteger("10432872995508747158655453911888929663232095726615177655775270714778607748931520761111401392500592252267944840074677554875523501126728719701952598345901097976713744390644019910302222421558965333712530353645091198999327135407641729347202656925038838513255173175382082058203709389775689188847546826799383611430857868823025994970382368066904910516234777673570629583115822604988500744128394637172447314983642478819070735310743101014571547333659308326755139764743712833006786721085185321683260034201727687232747763888621839603467628609300264399120101181136915268098261150348591689809740624250649391020790221680432465961961");
	private static final int mDHExponentSize = 0;

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
			Log.e(CLASS, String.format("Could not acquire Diffie-Hellman key pair generator " +
									   "to exchange session key with '%s': %s", mName, e.getMessage()));
//		} catch (NoSuchProviderException e) {
//			//TODO: Inform user via UI
//			Log.e(CLASS, String.format("Could not acquire Diffie-Hellman key pair generator " +
//									   "from SpongyCastle provider to exchange session key with '%s': %s", 
//									   mName, e.getMessage()));
		} catch (InvalidAlgorithmParameterException e) {
			//TODO: Inform user via UI
			Log.e(CLASS, String.format("Could not initialize key pair generator to exchange " +
									   "session key with '%s': %s", mName, e.getMessage()));
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
			Log.d(CLASS, String.format("Generated shared secret: %s [%d bits]", Base64.encodeBytes(mSharedSecret), mSharedSecret.length * 8));

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
