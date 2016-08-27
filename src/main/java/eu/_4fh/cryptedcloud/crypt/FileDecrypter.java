package eu._4fh.cryptedcloud.crypt;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.abstractj.kalium.crypto.Hash;
import org.abstractj.kalium.crypto.SealedBox;
import org.abstractj.kalium.crypto.SecretBox;
import org.abstractj.kalium.keys.KeyPair;
import org.eclipse.jdt.annotation.NonNull;

import eu._4fh.cryptedcloud.util.Util;

public class FileDecrypter extends InputStream {
	private final @NonNull Collection<KeyPair> keyPairs;
	private final @NonNull InputStream in;
	private byte[] buff;
	private int readBytes;
	private final @NonNull SecretBox secretBox;
	private final byte[] nextNonce;

	public FileDecrypter(final @NonNull InputStream in, final @NonNull Collection<KeyPair> keyPairs)
			throws IOException {
		super();
		if (keyPairs.size() <= 0 || keyPairs.size() >= Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Cant use KeyList with " + keyPairs.size() + " keys.");
		}
		this.in = in;
		this.keyPairs = Util.checkNonNull(Collections.unmodifiableCollection(keyPairs));
		this.buff = null;

		DataInputStream dataIn = new DataInputStream(this.in);
		secretBox = new SecretBox(readSymKey(dataIn));
		nextNonce = new byte[FileConstants.SYM_NONCE_SIZE];
		dataIn.readFully(nextNonce);
	}

	private byte[] readSymKey(DataInput in) throws IOException, EOFException {
		int numKeys = in.readInt();
		Map<KeyPair, byte[]> pubKeyHashes = new HashMap<KeyPair, byte[]>();
		for (KeyPair keyPair : keyPairs) {
			pubKeyHashes.put(keyPair, new Hash().blake2(keyPair.getPublicKey().toBytes()));
		}
		byte[] readKeyHash = new byte[FileConstants.HASH_SIZE];
		byte[] readSymKey = new byte[FileConstants.ENCRYPTED_SYM_KEY_SIZE];
		byte[] symKey = null;
		for (int keyPos = 0; keyPos < numKeys; keyPos++) {
			in.readFully(readKeyHash);
			for (Map.Entry<KeyPair, byte[]> keyEntry : pubKeyHashes.entrySet()) {
				if (Arrays.equals(keyEntry.getValue(), readKeyHash)) {
					in.readFully(readSymKey);
					SealedBox box = new SealedBox(keyEntry.getKey().getPublicKey().toBytes(),
							keyEntry.getKey().getPrivateKey().toBytes());
					symKey = box.decrypt(readSymKey);
					// To skip over remaining keys, simply keep reading...
					// Not the best performance but less code
				} else {
					in.readFully(readSymKey);
				}
			}
		}
		if (symKey == null) {
			throw new NoSymKeyException();
		}
		return symKey;
	}

	static public class NoSymKeyException extends IOException {
		private static final long serialVersionUID = -2000190731438580281L;

		public NoSymKeyException() {
			super();
		}

		public NoSymKeyException(String message) {
			super(message);
		}
	}

	@Override
	public int read() throws IOException {
		if (buff == null || readBytes >= buff.length) {
			if (!readNextChunk()) {
				return -1;
			}
		}
		readBytes++;
		return buff[readBytes - 1];
	}

	private boolean readNextChunk() throws IOException {
		DataInputStream in = new DataInputStream(this.in);
		int fileChunkSize = 0;
		try {
			fileChunkSize = in.readInt();
		} catch (EOFException e) {
			return false;
		}
		if (fileChunkSize <= 0) {
			throw new RuntimeException("Tried to get next fileChunkSize from file, but read " + fileChunkSize);
		}

		byte[] tmp = new byte[fileChunkSize];
		in.readFully(tmp);
		tmp = secretBox.decrypt(nextNonce, tmp);
		int nonceStartPos = tmp.length - FileConstants.SYM_NONCE_SIZE;
		System.arraycopy(tmp, nonceStartPos, nextNonce, 0, FileConstants.SYM_NONCE_SIZE);
		buff = new byte[nonceStartPos];
		System.arraycopy(tmp, 0, buff, 0, buff.length);
		readBytes = 0;
		return true;
	}

	@Override
	public int available() throws IOException {
		if (buff == null) {
			return 0;
		}
		return buff.length - readBytes;
	}
}
