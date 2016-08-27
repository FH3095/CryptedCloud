package eu._4fh.cryptedcloud.test.crypt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.encoders.Encoder;
import org.abstractj.kalium.keys.KeyPair;
import org.abstractj.kalium.keys.PrivateKey;

import eu._4fh.cryptedcloud.crypt.FileDecrypter;
import eu._4fh.cryptedcloud.crypt.FileEncrypter;
import eu._4fh.cryptedcloud.test.TestUtils;
import eu._4fh.cryptedcloud.util.Util;

public class CryptTestUtils {
	static public final String LARGE_FILE_PREFIX = "Test_Large";
	static public final String NORMAL_FILE_PREFIX = "Test_Normal";
	static public final String[] FILE_PREFIXES = new String[] { LARGE_FILE_PREFIX, NORMAL_FILE_PREFIX };
	static public final String[] FILE_POSTFIXES = new String[] { "_input.txt", "_encrypted.txt", "_decrypted.txt",
			"_decrypted1.txt", "_decrypted2.txt" };
	static public final byte[] aliceKey = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, };

	private CryptTestUtils() {
	}

	static public void createFile(final File file, final int size) throws IOException {
		try (final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file), 1 * 1024 * 1024)) {
			final byte[] charArray = new byte[10];
			for (int i = 0; i < 10; ++i) {
				charArray[i] = Integer.toString(i).getBytes()[0];
			}
			for (int i = 1; i <= size; ++i) {
				out.write(charArray[i % 10]);
				// one Line per 100 KB
				if (i % (1000 * 100) == 0) {
					out.write("\r\n".getBytes());
				}
			}
		}
	}

	static public boolean compareFiles(final File file1, final File file2) throws IOException {
		try (FileInputStream in1 = new FileInputStream(file1); FileInputStream in2 = new FileInputStream(file2);) {
			byte[] buff1 = new byte[50 * 1024 * 1024];
			byte[] buff2 = new byte[50 * 1024 * 1024];
			int read1 = 0;
			int read2 = 0;
			while (true) {
				read1 = Util.readAsMuchFileAsPossible(in1, buff1, 0, buff1.length);
				read2 = Util.readAsMuchFileAsPossible(in2, buff2, 0, buff2.length);
				if (read1 != read2) {
					break;
				}
				for (int i = 0; i < read1; ++i) {
					if (buff1[i] != buff2[i]) {
						break;
					}
				}
				if (read1 == -1 && read2 == -1) {
					return true;
				}
			}
			return false;
		}
	}

	public static void encryptFile(final String filePrefix, final KeyPair keyPair, final Random randomGenerator)
			throws IOException {
		try (@SuppressWarnings("null")
		OutputStream fos = new FileEncrypter(
				new BufferedOutputStream(new FileOutputStream(new File(filePrefix + "_encrypted.txt"))),
				Collections.singleton(keyPair.getPublicKey()))) {
			TestUtils.setField(fos, "random", randomGenerator);
			Util.writeFileToStream(new File(filePrefix + "_input.txt"), fos);
		}
	}

	public static void decryptFile(final String filePrefix, final KeyPair keyPair) throws IOException {
		try (@SuppressWarnings("null")
		InputStream fis = new FileDecrypter(
				new BufferedInputStream(new FileInputStream(new File(filePrefix + "_encrypted.txt"))),
				Collections.singleton(keyPair))) {
			Util.writeStreamToFile(fis, new File(filePrefix + "_decrypted.txt"));
		}
	}

	static public void clearFiles() {
		System.runFinalization();
		for (String filePrefix : CryptTestUtils.FILE_PREFIXES) {
			for (String filePostfix : CryptTestUtils.FILE_POSTFIXES) {
				File file = new File(filePrefix + filePostfix);
				if (!file.delete()) {
					file.deleteOnExit();
				}
			}
		}
	}

	static public boolean testFileContainsUnencryptedKey(final String filePrefix, final PrivateKey privateKey)
			throws IOException {
		try (InputStream in = new BufferedInputStream(new FileInputStream(new File(filePrefix + "_encrypted.txt")))) {
			byte[] fileContent = new byte[1024 * 1024];
			int read = Util.readAsMuchFileAsPossible(in, fileContent);
			byte[] keyRaw = privateKey.toBytes();
			byte[] keyHex = Encoder.HEX.encode(keyRaw).getBytes(StandardCharsets.US_ASCII);
			for (int i = 0; i < (read - keyRaw.length) && i < (read - keyHex.length); ++i) {
				boolean rawMatches = true;
				boolean hexMatches = true;
				for (int j = 0; j < keyRaw.length; j++) {
					if (rawMatches && fileContent[i + j] != keyRaw[j]) {
						rawMatches = false;
					}
					if (hexMatches && fileContent[i + j] != keyHex[j]) {
						hexMatches = false;
					}
				}
				if (rawMatches || hexMatches) {
					return true;
				}
			}
		}
		return false;
	}
}
