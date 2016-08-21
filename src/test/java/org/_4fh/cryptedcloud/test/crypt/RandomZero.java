package org._4fh.cryptedcloud.test.crypt;
import java.util.Arrays;

import org.abstractj.kalium.crypto.Random;

public class RandomZero extends Random {
	private static final int DEFAULT_SIZE = 32;

	public byte[] randomBytes(int n) {
		byte[] buffer = new byte[n];
		Arrays.fill(buffer, (byte) 0);
		return buffer;
	}

	public byte[] randomBytes() {
		byte[] buffer = new byte[DEFAULT_SIZE];
		Arrays.fill(buffer, (byte) 0);
		return buffer;
	}
}
