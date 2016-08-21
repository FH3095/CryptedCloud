package eu._4fh.cryptedcloud.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

public class Util {
	private static final Logger log = Util.getLogger();

	static public byte[] concatByteArrays(byte[] in1, int in1Src, int in1Len, byte[] in2, int in2Src, int in2Len) {
		byte[] result = new byte[in1Len + in2Len];
		System.arraycopy(in1, in1Src, result, 0, in1Len);
		System.arraycopy(in2, in2Src, result, in1Len, in2Len);
		return result;
	}

	static public int readAsMuchFileAsPossible(final InputStream in, final byte[] data) throws IOException {
		return readAsMuchFileAsPossible(in, data, 0, data.length);
	}

	static public int readAsMuchFileAsPossible(final InputStream in, final byte[] data, final int dataStart,
			final int dataLen) throws IOException {
		int totalReadBytes = 0;
		while (totalReadBytes < dataLen) {
			int curReadBytes = in.read(data, dataStart + totalReadBytes, dataLen - totalReadBytes);
			if (curReadBytes < 0) {
				if (totalReadBytes == 0) {
					return curReadBytes;
				}
				return totalReadBytes;
			}
			totalReadBytes += curReadBytes;
		}
		return totalReadBytes;
	}

	public static class Pair<V1, V2> {
		public final V1 value1;
		public final V2 value2;

		public Pair(V1 value1, V2 value2) {
			this.value1 = value1;
			this.value2 = value2;
		}

		@Override
		public String toString() {
			return "Util.Pair [value1=" + value1 + ", value2=" + value2 + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((value1 == null) ? 0 : value1.hashCode());
			result = prime * result + ((value2 == null) ? 0 : value2.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof Pair)) {
				return false;
			}
			@SuppressWarnings("rawtypes")
			Pair other = (Pair) obj;
			if (value1 == null) {
				if (other.value1 != null) {
					return false;
				}
			} else if (!value1.equals(other.value1)) {
				return false;
			}
			if (value2 == null) {
				if (other.value2 != null) {
					return false;
				}
			} else if (!value2.equals(other.value2)) {
				return false;
			}
			return true;
		}
	}

	public static @Nonnull Logger getLogger() {
		String className = Thread.currentThread().getStackTrace()[2].getClassName();
		return Logger.getLogger(className);
	}

	public static @Nonnull Logger getLogger(Class<?> clazz) {
		return Logger.getLogger(clazz.getCanonicalName());
	}

	public static void deleteFile(final @Nonnull File localFile) {
		boolean fileDeleted = false;
		try {
			fileDeleted = localFile.delete();
		} catch (Throwable t) {
			log.log(Level.WARNING,
					"Cant delete file \'" + localFile.getAbsolutePath() + "\'. Setting fileDeleteOnExit.", t);
		}
		if (!fileDeleted) {
			localFile.deleteOnExit();
		}
	}
}
