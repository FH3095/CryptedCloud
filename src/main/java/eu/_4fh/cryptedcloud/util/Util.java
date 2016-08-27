package eu._4fh.cryptedcloud.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import eu._4fh.cryptedcloud.config.Config;

public class Util {
	private static final Logger log = Util.getLogger();
	private static final int FILE_COPY_STEP_SIZE = 2097152; // 2MB

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

	public static class Pair<T1, T2> {
		public final T1 value1;
		public final T2 value2;

		public Pair(T1 value1, T2 value2) {
			this.value1 = value1;
			this.value2 = value2;
		}

		@Override
		public String toString() {
			return "Util.Pair [value1=" + value1 + ", value2=" + value2 + "]";
		}

		@SuppressWarnings("null")
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((value1 == null) ? 0 : value1.hashCode());
			result = prime * result + ((value2 == null) ? 0 : value2.hashCode());
			return result;
		}

		@SuppressWarnings("null")
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

	public static @NonNull Logger getLogger() {
		String className = Thread.currentThread().getStackTrace()[2].getClassName();
		return Util.checkNonNull(Logger.getLogger(className));
	}

	public static @NonNull Logger getLogger(Class<?> clazz) {
		return Util.checkNonNull(Logger.getLogger(clazz.getCanonicalName()));
	}

	public static void deleteTempFile(final @NonNull File localFile) {
		if (!localFile.getAbsolutePath().startsWith(Config.getInstance().getTempDir().getAbsolutePath())) {
			return;
		}
		deleteFile(localFile);
	}

	public static void deleteFile(final @NonNull File localFile) {
		if (localFile.isDirectory()) {
			throw new IllegalArgumentException(
					"Folder \"" + localFile.getAbsolutePath() + "\" is a folder, not a file!");
		}
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

	public static void deleteFolder(final @NonNull File localFile) throws IOException {
		if (!localFile.isDirectory()) {
			throw new IllegalArgumentException("File \"" + localFile.getAbsolutePath() + "\" is a file, not a folder!");
		}
		Files.walkFileTree(localFile.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(final Path file, final IOException e) throws IOException {
				return handleException(e);
			}

			private FileVisitResult handleException(final IOException e) throws IOException {
				log.log(Level.SEVERE, "Error while deleting folder \"" + localFile.getAbsolutePath() + "\": ", e);
				throw e;
			}

			@Override
			public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
				if (e != null)
					return handleException(e);
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	};

	public static void writeFileToStream(final @NonNull File inFile, final @NonNull OutputStream out)
			throws IOException {
		try (InputStream in = new FileInputStream(inFile)) {
			byte[] buff = new byte[FILE_COPY_STEP_SIZE];
			int read;
			while (true) {
				read = in.read(buff);
				if (read < 0) {
					break;
				}
				out.write(buff, 0, read);
			}
		}
	}

	public static void writeStreamToFile(final @NonNull InputStream in, final @NonNull File outFile)
			throws IOException {
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile))) {
			byte[] buff = new byte[FILE_COPY_STEP_SIZE];
			int read;
			while (true) {
				read = in.read(buff);
				if (read < 0) {
					break;
				}
				out.write(buff, 0, read);
			}
		}
	}

	public static <T> @NonNull T checkNonNull(final @Nullable T obj) {
		if (obj == null) {
			throw new NullPointerException("Obj is null!");
		}
		return obj;
	}
}
