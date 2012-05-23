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

	private static final BigInteger mDHPrime = new BigInteger("815574232680803154206887766914510389500646967323010707368182206183517307108837998758235786134286335327240774712833029987507579108062266986751451620132050062475230782514640852053366574887483924926661926205167668562829918544505321616953539664595727205384949539260097518703209084870203044125657602782446131145683554172486001866549178309694310106063661092517122110382537839463373573361744487935944360736263952105409562075349756877311399056431454693700694430672699524242871565792631851197368519879146662329646469634971457578704598192157048591249224050918639104095781014098273615086115261308273671008552294148987162728485735769966143595392948036980053828903194141623176098122982368876591378415147601923750551807820702753195504537551795391464766162513071723553353528696207119271161992764538347729050299801670117695002377699945180594568956125423409386704104270710941896620220624675434286689442401653908998197427855909515473156927997138621065904420615127164686627532131678511366319332246455297932626094169409631535401655495432046749601602883739085969990006563757215054708602704371284772136395599270860697287793391689496350718363499289587420208383411177480413658640271593925241109213210356617220396119233864670556285152465248938241453891555059");
	private static final BigInteger mDHGenerator = new BigInteger("774454156960413627543321968256920515957089803800734737840097590983772021308791926308539372574557079357855587542383048900673105723140351540817564597282587185244328093739250430084433910731768216236924189432412756455921574685252245253680185156625297844665962392054169125715281085733088333724146613940642764394742302530064817822276562860168268207264052147308987862544061418910149901701991706644537942547142100778842478386112579291736352463050754530066801373148512703387420731289643699927857582119364948385601677087729199369717000899282941834310419250511081938141438659433467565919569244399963038600098681748753148025013599523503124070341483017811274543506861926924575993248213375867240481412032152801300346992569244140340893780036428630453949001199523028438346890699515146794963878290774072158184572966914079784619749275275245758637042041603968140161504905545346510264029565868514692624930892470160610797571111625412073248120538297287119031714489611973738613969486696119347451859264851335887460500804823283222963661428281312238014144731112790458773381988465186633967636725183698757384546570629727331526476355724351861229103630025829931127279938739190020697681553421038290623945890249596354416827170642452304398278859252677276951254825573");
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
