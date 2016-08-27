package eu._4fh.cryptedcloud.test.crypt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedList;

import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.keys.KeyPair;
import org.abstractj.kalium.keys.PublicKey;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.crypt.FileDecrypter;
import eu._4fh.cryptedcloud.crypt.FileEncrypter;
import eu._4fh.cryptedcloud.util.Util;

public class TestDecryption {
	@BeforeClass
	static public void readConfig() throws IOException {
		try {
			Config.getInstance();
		} catch (IllegalStateException e) {
			Config.readConfig();
		}
	}

	@Test
	public void testRandomEncryption() throws IOException {
		CryptTestUtils.createFile(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_input.txt"), 100 * 1000);
		KeyPair keyPair = new KeyPair();
		CryptTestUtils.encryptFile(CryptTestUtils.NORMAL_FILE_PREFIX, keyPair, new Random());
		CryptTestUtils.decryptFile(CryptTestUtils.NORMAL_FILE_PREFIX, keyPair);
		assertFalse("Key in File", CryptTestUtils.testFileContainsUnencryptedKey(CryptTestUtils.NORMAL_FILE_PREFIX,
				keyPair.getPrivateKey()));
		assertTrue("Decrypted!=Encrypted",
				CryptTestUtils.compareFiles(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_input.txt"),
						new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_decrypted.txt")));
	}

	@Test
	public void testEncryptionLarge() throws IOException {
		CryptTestUtils.createFile(new File(CryptTestUtils.LARGE_FILE_PREFIX + "_input.txt"), 110 * 1024 * 1024);
		KeyPair keyPair = new KeyPair(CryptTestUtils.aliceKey);
		CryptTestUtils.encryptFile(CryptTestUtils.LARGE_FILE_PREFIX, keyPair, new RandomZero());
		CryptTestUtils.decryptFile(CryptTestUtils.LARGE_FILE_PREFIX, keyPair);
		assertFalse("Key in File", CryptTestUtils.testFileContainsUnencryptedKey(CryptTestUtils.LARGE_FILE_PREFIX,
				keyPair.getPrivateKey()));
		assertTrue("Decrypted!=Encrypted",
				CryptTestUtils.compareFiles(new File(CryptTestUtils.LARGE_FILE_PREFIX + "_input.txt"),
						new File(CryptTestUtils.LARGE_FILE_PREFIX + "_decrypted.txt")));
	}

	@Test
	public void testDifferentKeyEncryptionDecryption() throws IOException {
		CryptTestUtils.createFile(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_input.txt"), 100 * 1000);
		KeyPair key1 = new KeyPair();
		KeyPair key2 = new KeyPair();
		LinkedList<PublicKey> keys = new LinkedList<PublicKey>();
		keys.add(key1.getPublicKey());
		keys.add(key2.getPublicKey());
		try (OutputStream fos = new FileEncrypter(new BufferedOutputStream(
				new FileOutputStream(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_encrypted.txt"))), keys)) {
			Util.writeFileToStream(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_input.txt"), fos);
		}
		try (@SuppressWarnings("null")
		InputStream fis = new FileDecrypter(
				new FileInputStream(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_encrypted.txt")),
				Collections.singleton(key1))) {
			Util.writeStreamToFile(fis, new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_decrypted1.txt"));
		}
		try (@SuppressWarnings("null")
		InputStream fis = new FileDecrypter(
				new FileInputStream(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_encrypted.txt")),
				Collections.singleton(key2))) {
			Util.writeStreamToFile(fis, new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_decrypted2.txt"));
		}
		assertTrue("Decrypted1!=Encrypted",
				CryptTestUtils.compareFiles(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_input.txt"),
						new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_decrypted1.txt")));
		assertTrue("Decrypted2!=Encrypted",
				CryptTestUtils.compareFiles(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_input.txt"),
						new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_decrypted2.txt")));
	}

	@Test
	public void testBinaryEncryptionByStep() throws IOException {
		File inputFile = new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_input.txt");
		CryptTestUtils.createFile(inputFile, 1);
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(inputFile))) {
			for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; ++i) {
				out.write(i);
			}
		}
		KeyPair keyPair = new KeyPair();
		CryptTestUtils.encryptFile(CryptTestUtils.NORMAL_FILE_PREFIX, keyPair, new Random());
		CryptTestUtils.decryptFile(CryptTestUtils.NORMAL_FILE_PREFIX, keyPair);
		assertFalse("Key in File", CryptTestUtils.testFileContainsUnencryptedKey(CryptTestUtils.NORMAL_FILE_PREFIX,
				keyPair.getPrivateKey()));
		try (@SuppressWarnings("null")
		InputStream fis = new FileDecrypter(
				new BufferedInputStream(
						new FileInputStream(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_encrypted.txt"))),
				Collections.singleton(keyPair));
				OutputStream out = new BufferedOutputStream(
						new FileOutputStream(CryptTestUtils.NORMAL_FILE_PREFIX + "_decrypted.txt"));) {
			byte expectedByte = Byte.MIN_VALUE;
			for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; ++i) {
				final int byteValue = fis.read();
				final int expected = expectedByte & 0xFF;
				assertTrue("Exptected " + expected + " but got " + byteValue, expected == byteValue);
				out.write(byteValue);
				expectedByte++;
			}
		}
		assertTrue("Decrypted!=Encrypted",
				CryptTestUtils.compareFiles(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_input.txt"),
						new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_decrypted.txt")));

	}

	@After
	public void cleanUp() {
		CryptTestUtils.clearFiles();
	}
}
