package eu._4fh.cryptedcloud.crypt;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.abstractj.kalium.crypto.Hash;
import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.crypto.SealedBox;
import org.abstractj.kalium.crypto.SecretBox;
import org.abstractj.kalium.keys.PublicKey;

import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.util.Util;

public class FileEncrypter {
	private List<PublicKey> keys;
	private Random random;

	public FileEncrypter() {
		keys = new LinkedList<PublicKey>();
		random = new Random();
	}

	public boolean addKey(PublicKey key) {
		if (keys.contains(key)) {
			return false;
		}
		if (keys.size() > 126) {
			return false;
		}
		keys.add(key);
		return true;
	}

	public boolean encryptFile(final FileInputStream in, final BufferedOutputStream outRaw)
			throws UnsupportedOperationException, IOException {
		if (keys.size() < 1) {
			throw new IllegalStateException("No Key to encrypt with!");
		}
		DataOutputStream out = new DataOutputStream(outRaw);
		byte[] symKey = random.randomBytes(FileConstants.SYM_KEY_SIZE);

		writeKeysToFile(out, symKey);
		encryptFile(in, out, symKey);

		return true;
	}

	private void encryptFile(final FileInputStream in, final DataOutputStream out, final byte[] symKey)
			throws IOException {
		final int fileChunkSize = Config.getInstance().getFileChunkSize();
		byte[] fileChunk = new byte[fileChunkSize + FileConstants.SYM_NONCE_SIZE];
		final SecretBox box = new SecretBox(symKey);

		byte[] nonce = random.randomBytes(FileConstants.SYM_NONCE_SIZE);
		byte[] nextNonce = random.randomBytes(FileConstants.SYM_NONCE_SIZE);
		out.write(nonce);
		int readBytes = 0;
		while ((readBytes = Util.readAsMuchFileAsPossible(in, fileChunk, 0, fileChunkSize)) != -1) {
			if (readBytes != fileChunkSize) {
				byte[] tmp = Util.concatByteArrays(fileChunk, 0, readBytes, nextNonce, 0, nextNonce.length);
				tmp = box.encrypt(nonce, tmp);
				out.writeInt(tmp.length);
				out.write(tmp);
			} else {
				System.arraycopy(nextNonce, 0, fileChunk, fileChunkSize, FileConstants.SYM_NONCE_SIZE);
				byte[] tmp = box.encrypt(nonce, fileChunk);
				out.writeInt(tmp.length);
				out.write(tmp);
			}
			nonce = nextNonce;
			nextNonce = random.randomBytes(FileConstants.SYM_NONCE_SIZE);
		}
	}

	private void writeKeysToFile(final DataOutputStream out, final byte[] symKey)
			throws UnsupportedOperationException, IOException {
		Hash hash = new Hash();
		out.writeInt(keys.size());
		for (PublicKey key : keys) {
			out.write(hash.blake2(key.toBytes()));
			SealedBox box = new SealedBox(key.toBytes());
			out.write(box.encrypt(symKey));
		}
	}
}
