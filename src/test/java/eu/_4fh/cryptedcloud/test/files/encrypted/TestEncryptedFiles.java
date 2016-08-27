package eu._4fh.cryptedcloud.test.files.encrypted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

import org.abstractj.kalium.keys.KeyPair;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.crypt.FileDecrypter;
import eu._4fh.cryptedcloud.files.CloudFile;
import eu._4fh.cryptedcloud.files.CloudFolder;
import eu._4fh.cryptedcloud.files.encrypted.EncryptedService;
import eu._4fh.cryptedcloud.files.raw.RawService;
import eu._4fh.cryptedcloud.test.TestUtils;
import eu._4fh.cryptedcloud.test.crypt.CryptTestUtils;
import eu._4fh.cryptedcloud.util.Util;

public class TestEncryptedFiles {
	private static final @NonNull File srcDir = new File("testSrcDir");
	private static final @NonNull File dstDir = new File("testDstDir");

	@Before
	public void initConfig() throws IOException {
		try {
			Config.getInstance();
		} catch (IllegalStateException e) {
			Config.readConfig();
		}
		TestUtils.setField(Config.getInstance(), "targetDir", dstDir.getAbsolutePath());
		srcDir.mkdir();
		dstDir.mkdir();
		assertTrue(srcDir.exists());
		assertTrue(srcDir.isDirectory());
		assertTrue(dstDir.exists());
		assertTrue(dstDir.isDirectory());
	}

	@SuppressWarnings("null")
	@Test
	public void testEncryptAndDecrypt() throws IOException {
		KeyPair keyPair = new KeyPair();
		File srcFile = new File(srcDir, "Input.txt");
		File resultFile = new File(srcDir, "Decrypted.txt");
		CryptTestUtils.createFile(srcFile, 110 * 1024 * 1024);
		EncryptedService service = new EncryptedService(new RawService(dstDir), Collections.singleton(keyPair),
				Collections.singleton(keyPair.getPublicKey()));
		service.startSync(true);
		CloudFolder rootFolder = service.getRootFolder();
		CloudFile dstFile = rootFolder.createFile("Test.txt");
		try (OutputStream out = dstFile.getOutputStream()) {
			Util.writeFileToStream(srcFile, out);
		}
		try (InputStream in = dstFile.getInputStream()) {
			Util.writeStreamToFile(in, resultFile);
		}
		service.finishSync(true, true);
		assertTrue("Encrypted!=Decrypted", CryptTestUtils.compareFiles(srcFile, resultFile));
	}

	@SuppressWarnings("null")
	@Test
	public void testPrependTimestamp() throws IOException, IllegalArgumentException, IllegalAccessException {
		final long timestamp = 0x1234567890ABCDEFL;
		final byte[] data = new byte[] { Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE };
		final KeyPair keyPair = new KeyPair();
		EncryptedService service = new EncryptedService(new RawService(dstDir), Collections.singleton(keyPair),
				Collections.singleton(keyPair.getPublicKey()));
		TestUtils.getSetableField(service, "doCompress").setBoolean(service, false);
		service.startSync(true);
		CloudFolder rootFolder = service.getRootFolder();
		CloudFile dstFile = rootFolder.createFile("Test.txt");
		try (OutputStream out = dstFile.getOutputStream()) {
			new DataOutputStream(out).writeLong(timestamp);
			out.flush();
			out.write(data);
		}
		try (InputStream in = dstFile.getInputStream()) {
			long returnedTimestamp = new DataInputStream(in).readLong();
			assertEquals(timestamp, returnedTimestamp);
			int bytesRead = TestUtils.getSetableField((FileDecrypter) in, "readBytes").getInt(in);
			assertEquals(bytesRead, Long.BYTES); // Make sure the FileDecrypter didnt read more bytes than necassary
			byte[] returnedData = new byte[data.length];
			Util.readAsMuchFileAsPossible(in, returnedData);
			assertArrayEquals(data, returnedData);
		}
		service.finishSync(true, true);
	}

	@After
	public void cleanUp() throws IOException {
		if (srcDir.exists()) {
			Util.deleteFolder(srcDir);
		}
		if (dstDir.exists()) {
			Util.deleteFolder(dstDir);
		}
	}
}
