package eu._4fh.cryptedcloud.crypt;

import org.abstractj.kalium.NaCl;

public class FileConstants {
	static public final int HASH_SIZE = NaCl.Sodium.CRYPTO_GENERICHASH_BLAKE2B_BYTES;
	static public final int SYM_NONCE_SIZE = NaCl.Sodium.CRYPTO_SECRETBOX_XSALSA20POLY1305_NONCEBYTES;
	static public final int SYM_KEY_SIZE = NaCl.Sodium.CRYPTO_SECRETBOX_XSALSA20POLY1305_KEYBYTES;
	static public final int ENCRYPTED_SYM_KEY_SIZE = NaCl.Sodium.CRYPTO_SECRETBOX_XSALSA20POLY1305_KEYBYTES
			+ NaCl.Sodium.CRYPTO_BOX_SEALBYTES;
	static public final int SCRYPT_OPSLIMIT = 33554432; // crypto_pwhash_scryptsalsa208sha256_OPSLIMIT_SENSITIVE
	static public final long SCRYPT_MEMLIMIT = 1073741824; // crypto_pwhash_scryptsalsa208sha256_MEMLIMIT_SENSITIVE
	static public final int SCRYPT_SALT_SIZE = 32; // Taken from
													// libsodium-master\src\libsodium\include\sodium\crypto_pwhash_scryptsalsa208sha256.h

	private FileConstants() {
	}
}
