package eu._4fh.cryptedcloud.crypt;

import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;

import org.abstractj.kalium.crypto.Hash;
import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.crypto.SealedBox;
import org.abstractj.kalium.crypto.SecretBox;
import org.abstractj.kalium.keys.PublicKey;
import org.eclipse.jdt.annotation.NonNull;

import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.util.Util;

public class FileEncrypter extends FilterOutputStream {
	private final @NonNull Collection<PublicKey> keys;
	private final @NonNull Random random;
	private final int chunkSize;
	private final byte[] buff;
	private int writtenBytes;
	private byte[] nextNonce;
	private final @NonNull SecretBox secretBox;

	public FileEncrypter(final @NonNull OutputStream out, final @NonNull Collection<PublicKey> keys)
			throws IOException {
		super(out);
		if (keys.size() <= 0 || keys.size() >= Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Cant use KeyList with " + keys.size() + " keys.");
		}
		this.keys = Util.checkNonNull(Collections.unmodifiableCollection(keys));
		this.random = new Random();
		this.nextNonce = random.randomBytes(FileConstants.SYM_NONCE_SIZE);
		this.chunkSize = Config.getInstance().getFileChunkSize();

		buff = new byte[this.chunkSize + FileConstants.SYM_NONCE_SIZE];
		writtenBytes = 0;

		final byte[] symKey = random.randomBytes(FileConstants.SYM_KEY_SIZE);
		secretBox = new SecretBox(symKey);

		writeFileHeader(symKey);
	}

	private void writeFileHeader(final byte[] symKey) throws IOException {
		DataOutputStream out = new DataOutputStream(this.out);

		Hash hash = new Hash();
		out.writeInt(keys.size());
		for (PublicKey key : keys) {
			out.write(hash.blake2(key.toBytes()));
			SealedBox box = new SealedBox(key.toBytes());
			out.write(box.encrypt(symKey));
		}
		out.write(nextNonce);
	}

	private void encryptChunk() throws IOException {
		DataOutputStream out = new DataOutputStream(this.out);
		byte[] nonce = nextNonce;
		nextNonce = random.randomBytes(FileConstants.SYM_NONCE_SIZE);

		byte[] tmp;
		if (writtenBytes != chunkSize) {
			tmp = Util.concatByteArrays(buff, 0, writtenBytes, nextNonce, 0, nextNonce.length);
		} else {
			System.arraycopy(nextNonce, 0, buff, writtenBytes, FileConstants.SYM_NONCE_SIZE);
			tmp = buff;
		}
		tmp = secretBox.encrypt(nonce, tmp);
		out.writeInt(tmp.length);
		out.write(tmp);
		writtenBytes = 0;
	}

	@Override
	public void flush() throws IOException {
		encryptChunk();
		out.flush();
	}

	@Override
	public void close() throws IOException {
		encryptChunk();
		try (OutputStream ostream = out) {
		}
	}

	@Override
	public void write(int b) throws IOException {
		if (b < Byte.MIN_VALUE || b > Byte.MAX_VALUE) {
			throw new IllegalArgumentException(
					"b is " + b + " but must be must be " + Byte.MIN_VALUE + " <= b <= " + Byte.MAX_VALUE);
		}
		buff[writtenBytes] = (byte) b;
		writtenBytes++;
		if (writtenBytes >= chunkSize) {
			encryptChunk();
		}
	}
}
