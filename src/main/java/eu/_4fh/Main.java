package eu._4fh;

import java.io.UnsupportedEncodingException;

import org.abstractj.kalium.NaCl;
import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.crypto.SealedBox;
import org.abstractj.kalium.crypto.SecretBox;
import org.abstractj.kalium.encoders.Encoder;
import org.abstractj.kalium.keys.KeyPair;

public class Main {
	public static void main(String[] args) {
		NaCl.init();

		try {
			byte[] msg = null;
			byte[] crypted = null;
			byte[] decrypted = null;

			KeyPair aliceKey = new KeyPair();
			SealedBox aliceBox = new SealedBox(aliceKey.getPublicKey().toBytes(), aliceKey.getPrivateKey().toBytes());
			SealedBox bobBox = new SealedBox(aliceKey.getPublicKey().toBytes());

			msg = "testAsymmetric ä".getBytes("UTF-8");
			crypted = bobBox.encrypt(msg);
			decrypted = aliceBox.decrypt(crypted);

			System.out.println(Encoder.RAW.encode(crypted));
			System.out.println(Encoder.HEX.encode(crypted));
			System.out.println(new String(decrypted, "UTF-8"));
			System.out.println("---------------------------------");

			msg = "testSymetric ä".getBytes("UTF-8");
			Random random = new Random();
			byte[] symKey = random.randomBytes(NaCl.Sodium.CRYPTO_SECRETBOX_XSALSA20POLY1305_KEYBYTES);
			byte[] nonce = random.randomBytes(NaCl.Sodium.CRYPTO_SECRETBOX_XSALSA20POLY1305_NONCEBYTES);
			SecretBox carolBox = new SecretBox(symKey);
			SecretBox daveBox = new SecretBox(symKey);
			crypted = carolBox.encrypt(nonce, msg);
			decrypted = daveBox.decrypt(nonce, crypted);

			System.out.println(Encoder.RAW.encode(crypted));
			System.out.println(Encoder.HEX.encode(crypted));
			System.out.println(new String(decrypted, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
}
