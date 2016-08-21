package org._4fh.cryptedcloud.test.crypt;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import eu._4fh.cryptedcloud.crypt.PasswordBasedEncryption;

public class TestPasswordBasedEncryption {
	private static final byte[] rawData = new byte[] { -101, -106, 55, -8, -75, -22, 8, 34, 61, -27, 100, 75, 105, 86,
			-9, 43, -46, 36, 90, -106, -110, 21, -49, 43, 60, 121, -125, -95, -36, -88, 0, 12, 1, -57, -71, 118, -68,
			21, -77, -12, 18, -37, 52, -46, 24, 120, -115, -99, -37, -94, -115, -118, 123, -18, -64, -22, 123, 40, 77,
			77, -17, -23, -5, -44, 46, -29, 28, 66, -76, 18, -96, -37, 53, -56, -91, -81, 31, -93, 122, 55, -112, 59,
			-14, 49, 111, 31, -60, -7, -4, 102, 95, -63, -109, -96, -30, -103, 117, 92, -45, -29, 35, -63, -128, -35,
			-89, 7, -116, 68, 10, -110, -32, 83, -90, 1, -23, -58, 98, -18, 63, 16, 73, -11, 76, -53, 119, -24, -106,
			102, -6, 47, -90, 0, -11, -16, 69, 120, -124, -15, -6, -8, -70, 119, -112, -41, 22, 54, 45, 116, 114, -65,
			91, -103, -24, -6, -65, -27, 116, -108, -71, -114, 127, 109, 85, 16, -73, 24, 82, -83, 93, 54, -108, -119,
			27, 49, -82, -64, -96, -115, 36, -114, 51, 0, -124, -58, 80, 104, -58, 33, 69, -26, 28, -40, -96, -110, 117,
			-55, -63, 66, -93, -108, 96, 120, 31, -51, -29, 98, 81, -71, 18, -1, -23, 94, 99, -22, 5, -30, 39, -26,
			-117, 24, -102, 53, 126, -41, -52, -103, 105, 26, 57 };
	private static final String password = "#]8q';S!ww}nL{1p@+g";

	@Test
	public void testEncryptionAndDecryption() {
		byte[] encrypted = new PasswordBasedEncryption().encrypt(password, rawData);
		byte[] decrypted = new PasswordBasedEncryption().decrypt(password, encrypted);
		assertTrue(Arrays.equals(rawData, decrypted));
	}
}
