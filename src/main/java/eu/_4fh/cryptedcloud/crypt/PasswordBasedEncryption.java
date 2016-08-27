package eu._4fh.cryptedcloud.crypt;

import java.nio.charset.StandardCharsets;

import org.abstractj.kalium.crypto.Password;
import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.crypto.SecretBox;
import org.eclipse.jdt.annotation.NonNull;

public class PasswordBasedEncryption {
	public PasswordBasedEncryption() {
	}

	public byte[] encrypt(final @NonNull String password, final byte[] data) {
		byte[] salt = new Random().randomBytes(FileConstants.SCRYPT_SALT_SIZE);
		byte[] key = new Password().deriveKey(FileConstants.SYM_KEY_SIZE, password.getBytes(StandardCharsets.UTF_8),
				salt, FileConstants.SCRYPT_OPSLIMIT, FileConstants.SCRYPT_MEMLIMIT);

		byte[] nonce = new Random().randomBytes(FileConstants.SYM_NONCE_SIZE);
		byte[] encrypted = new SecretBox(key).encrypt(nonce, data);

		byte[] result = new byte[FileConstants.SCRYPT_SALT_SIZE + FileConstants.SYM_NONCE_SIZE + encrypted.length];
		System.arraycopy(salt, 0, result, 0, salt.length);
		System.arraycopy(nonce, 0, result, salt.length, nonce.length);
		System.arraycopy(encrypted, 0, result, salt.length + nonce.length, encrypted.length);
		return result;
	}

	public byte[] decrypt(final @NonNull String password, final byte[] data) {
		byte[] salt = new byte[FileConstants.SCRYPT_SALT_SIZE];
		byte[] nonce = new byte[FileConstants.SYM_NONCE_SIZE];
		byte[] encrypted = new byte[data.length - (salt.length + nonce.length)];
		System.arraycopy(data, 0, salt, 0, salt.length);
		System.arraycopy(data, salt.length, nonce, 0, nonce.length);
		System.arraycopy(data, salt.length + nonce.length, encrypted, 0, encrypted.length);

		byte[] key = new Password().deriveKey(FileConstants.SYM_KEY_SIZE, password.getBytes(StandardCharsets.UTF_8),
				salt, FileConstants.SCRYPT_OPSLIMIT, FileConstants.SCRYPT_MEMLIMIT);

		byte[] result = new SecretBox(key).decrypt(nonce, encrypted);
		return result;
	}
}
