package eu._4fh.cryptedcloud.cloud.encrypted;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import eu._4fh.cryptedcloud.cloud.CloudFile;
import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.util.Util;

public class EncryptedFile implements CloudFile {
	private static final Logger log = Util.getLogger();
	private final CloudFile file;
	private final EncryptedCloud cloud;
	private long lastModified;

	EncryptedFile(final @Nonnull EncryptedCloud cloud, final @Nonnull CloudFile file) {
		this.cloud = cloud;
		this.file = file;
		lastModified = -1;
	}

	@Override
	public @Nonnull String getFileName() {
		return cloud.getNameEncrypter().decryptName(file.getFileName());
	}

	@Override
	public void updateFile(@Nonnull File file) throws IOException {
		lastModified = -1;
		File encryptedFile = File.createTempFile(file.getName(), ".crypted", Config.getInstance().getTempDir());
		try {
			cloud.prependTimestampAndEncryptFile(encryptedFile, file);
			file = null;
			this.file.updateFile(encryptedFile);
		} finally {
			Util.deleteFile(encryptedFile);
		}
	}

	@Override
	public void downloadFile(final @Nonnull File localFile) throws IOException {
		downloadFile(localFile, 0, CloudFile.LAST_BYTES_UNDEF_VALUE);
	}

	@Override
	public void downloadFile(final @Nonnull File localFile, int firstByte, int lastByte) throws IOException {
		firstByte += EncryptedCloud.FILE_CONTENT_OFFSET;
		if (lastByte != CloudFile.LAST_BYTES_UNDEF_VALUE) {
			lastByte += EncryptedCloud.FILE_CONTENT_OFFSET;
		}
		File encryptedFile = File.createTempFile(localFile.getName(), ".crypted", Config.getInstance().getTempDir());
		try {
			this.file.downloadFile(encryptedFile, firstByte, lastByte);
			cloud.decryptFile(localFile, encryptedFile);
		} finally {
			Util.deleteFile(encryptedFile);
		}
	}

	@Nonnull
	CloudFile getCloudFile() {
		return file;
	}

	@Override
	public long getLastModification() throws IOException {
		if (lastModified == -1) {
			File tempFile = File.createTempFile(getFileName(), ".timestamp", Config.getInstance().getTempDir());
			try {
				file.downloadFile(tempFile, 0, EncryptedCloud.FILE_CONTENT_OFFSET);
				try (DataInputStream stream = new DataInputStream(new FileInputStream(tempFile))) {
					lastModified = stream.readLong();
					log.finest(() -> "Reading last modified for remote file " + getFileName() + ": " + lastModified);
				}
			} finally {
				Util.deleteFile(tempFile);
			}
		}
		return lastModified;
	}
}
