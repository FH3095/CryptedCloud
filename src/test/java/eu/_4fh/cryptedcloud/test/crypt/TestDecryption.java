package eu._4fh.cryptedcloud.test.crypt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.keys.KeyPair;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.crypt.FileDecrypter;
import eu._4fh.cryptedcloud.crypt.FileEncrypter;

public class TestDecryption {
	@BeforeClass
	static public void readConfig() throws IOException {
		Config.readConfig();
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
		try (FileInputStream fis = new FileInputStream(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_input.txt"));
				BufferedOutputStream fos = new BufferedOutputStream(
						new FileOutputStream(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_encrypted.txt")));) {
			FileEncrypter encrypter = new FileEncrypter();
			encrypter.addKey(key1.getPublicKey());
			encrypter.addKey(key2.getPublicKey());
			assertTrue(encrypter.encryptFile(fis, fos));
		}
		try (FileInputStream fis = new FileInputStream(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_encrypted.txt"));
				BufferedOutputStream fos = new BufferedOutputStream(
						new FileOutputStream(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_decrypted1.txt")));) {
			FileDecrypter decrypter = new FileDecrypter(key1);
			assertTrue(decrypter.decryptFile(fis, fos));
		}
		try (FileInputStream fis = new FileInputStream(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_encrypted.txt"));
				BufferedOutputStream fos = new BufferedOutputStream(
						new FileOutputStream(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_decrypted2.txt")));) {
			FileDecrypter decrypter = new FileDecrypter(key2);
			assertTrue(decrypter.decryptFile(fis, fos));
		}
		assertTrue("Decrypted1!=Encrypted",
				CryptTestUtils.compareFiles(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_input.txt"),
						new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_decrypted1.txt")));
		assertTrue("Decrypted2!=Encrypted",
				CryptTestUtils.compareFiles(new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_input.txt"),
						new File(CryptTestUtils.NORMAL_FILE_PREFIX + "_decrypted2.txt")));
	}

	@After
	public void cleanUp() {
		CryptTestUtils.clearFiles();
	}
}
