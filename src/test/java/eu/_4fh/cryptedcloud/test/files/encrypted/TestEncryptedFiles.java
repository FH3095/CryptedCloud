package eu._4fh.cryptedcloud.test.files.encrypted;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.test.TestUtils;
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
		assertTrue(srcDir.mkdir());
		assertTrue(dstDir.mkdir());
	}

	@Test
	public void testEncryptAndDecrypt() {
		// FIXME Write Test
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
