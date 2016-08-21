package eu._4fh.cryptedcloud.crypt;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.abstractj.kalium.crypto.Hash;
import org.abstractj.kalium.crypto.SealedBox;
import org.abstractj.kalium.crypto.SecretBox;
import org.abstractj.kalium.keys.KeyPair;

public class FileDecrypter {
	private List<KeyPair> keyPairs;

	public FileDecrypter() {
		keyPairs = new LinkedList<KeyPair>();
	}

	public FileDecrypter(final KeyPair key) {
		if (key == null) {
			throw new IllegalArgumentException("PrivateKey expected");
		}
		keyPairs = new LinkedList<KeyPair>();
		keyPairs.add(key);
	}

	public boolean addKey(KeyPair keyPair) {
		if (keyPairs.contains(keyPair)) {
			return false;
		}
		if (keyPairs.size() > 126) {
			return false;
		}
		keyPairs.add(keyPair);
		return true;
	}

	public boolean decryptFile(final InputStream inRaw, final BufferedOutputStream out)
			throws IOException, EOFException {
		if (keyPairs.size() < 1) {
			throw new IllegalStateException("No Key to decrypt with!");
		}
		final DataInputStream in = new DataInputStream(inRaw);
		byte[] symKey = readSymKey(in);
		if (symKey == null) {
			return false;
		}
		decryptFile(in, out, symKey);
		return true;
	}

	private byte[] readSymKey(final DataInput in) throws IOException, EOFException {
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
		return symKey;
	}

	private void decryptFile(final DataInput in, final BufferedOutputStream out, final byte[] symKey)
			throws IOException, EOFException {
		SecretBox box = new SecretBox(symKey);
		byte[] nonce = new byte[FileConstants.SYM_NONCE_SIZE];
		byte[] fileChunk = null;
		in.readFully(nonce);
		while (true) {
			int fileChunkSize = 0;
			try {
				fileChunkSize = in.readInt();
			} catch (EOFException e) {
				break;
			}
			if (fileChunk == null || fileChunk.length != fileChunkSize) {
				fileChunk = new byte[fileChunkSize];
			}
			in.readFully(fileChunk);
			byte[] tmp = box.decrypt(nonce, fileChunk);
			int nonceStartPos = tmp.length - FileConstants.SYM_NONCE_SIZE;
			out.write(tmp, 0, nonceStartPos);
			System.arraycopy(tmp, nonceStartPos, nonce, 0, FileConstants.SYM_NONCE_SIZE);
		}
	}
}
