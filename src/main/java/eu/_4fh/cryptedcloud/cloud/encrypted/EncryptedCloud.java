package eu._4fh.cryptedcloud.cloud.encrypted;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import eu._4fh.cryptedcloud.cloud.CloudFile;
import eu._4fh.cryptedcloud.cloud.CloudFolder;
import eu._4fh.cryptedcloud.cloud.CloudService;
import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.crypt.FileDecrypter;
import eu._4fh.cryptedcloud.crypt.FileEncrypter;
import eu._4fh.cryptedcloud.crypt.FileNameEncrypter;
import eu._4fh.cryptedcloud.util.Util;
import eu._4fh.cryptedcloud.util.Util.Pair;

public class EncryptedCloud implements CloudService {
	private static final Logger log = Util.getLogger();
	private static final String ENCRYPTED_FOLDER_NAME = "EncFolder";
	static final int FILE_CONTENT_OFFSET = Long.BYTES;
	private final @Nonnull CloudService cloud;
	private static final @Nonnull String NAME_ENCRYPTER_DATABASE_FILENAME = "FileNameEncrypter.keystore";
	private final @Nonnull FileEncrypter fileEncrypter;
	private final @Nonnull FileDecrypter fileDecrypter;
	private final @Nonnull FileNameEncrypter nameEncrypter;
	private final @Nonnull EncryptedFolder rootFolder;

	public EncryptedCloud(final @Nonnull CloudService cloud, final @Nonnull FileEncrypter fileEncrypter,
			final @Nonnull FileDecrypter fileDecrypter) throws IOException {
		this.cloud = cloud;
		this.fileEncrypter = fileEncrypter;
		this.fileDecrypter = fileDecrypter;
		nameEncrypter = new FileNameEncrypter();
		CloudFile nameEncrypterCloudFile = cloud.getRootFolder().getFiles().get(NAME_ENCRYPTER_DATABASE_FILENAME);
		if (nameEncrypterCloudFile != null) {
			initNameEncrypterFromFile(nameEncrypterCloudFile);
		} else {
			nameEncrypter.initializeClean();
		}
		String encryptedFolderName = nameEncrypter.encryptName(ENCRYPTED_FOLDER_NAME);
		CloudFolder encryptedFolder = cloud.getRootFolder().getSubFolders().get(encryptedFolderName);
		if (encryptedFolder == null) {
			encryptedFolder = cloud.getRootFolder().createFolder(encryptedFolderName);
		}
		rootFolder = new EncryptedFolder(this, encryptedFolder);
	}

	private void initNameEncrypterFromFile(CloudFile nameEncrypterCloudFile) throws IOException {
		File cryptedFile = File.createTempFile("FileNameEncrypter", ".crypted", Config.getInstance().getTempDir());
		File decryptedFile = File.createTempFile("FileNameEncrypter", ".tmp", Config.getInstance().getTempDir());
		try {
			nameEncrypterCloudFile.downloadFile(cryptedFile, FILE_CONTENT_OFFSET, CloudFile.LAST_BYTES_UNDEF_VALUE);
			decryptFile(decryptedFile, cryptedFile);
			nameEncrypter.initializeFromFile(decryptedFile);
		} finally {
			Util.deleteFile(decryptedFile);
			Util.deleteFile(cryptedFile);
		}
	}

	@Override
	public @Nonnull CloudFolder getRootFolder() {
		return rootFolder;
	}

	@Override
	public @Nonnull String getUserName() {
		return cloud.getUserName();
	}

	@Override
	public @Nonnull Pair<Long, Long> getUsageAndLimit(boolean refresh) throws IOException {
		return cloud.getUsageAndLimit(refresh);
	}

	@Override
	public @Nonnull Long getFreeSpace(boolean refresh) throws IOException {
		return cloud.getFreeSpace(refresh);
	}

	@Nonnull
	FileNameEncrypter getNameEncrypter() {
		return nameEncrypter;
	}

	@Override
	public void finishSync(final boolean upload, final boolean wasSuccessfull) throws IOException {
		if (!upload) {
			return;
		}
		File encryptedFile = File.createTempFile("FileNameEncrypter", ".crypted", Config.getInstance().getTempDir());
		File decryptedFile = File.createTempFile("FileNameEncrypter", ".tmp", Config.getInstance().getTempDir());
		try {
			nameEncrypter.writeToFile(decryptedFile);
			prependTimestampAndEncryptFile(encryptedFile, decryptedFile);
			CloudFile cloudFile = cloud.getRootFolder().getFiles().get(NAME_ENCRYPTER_DATABASE_FILENAME);
			if (cloudFile != null) {
				cloudFile.updateFile(encryptedFile);
			} else {
				cloud.getRootFolder().createFile(NAME_ENCRYPTER_DATABASE_FILENAME, encryptedFile);
			}
		} finally {
			Util.deleteFile(encryptedFile);
			Util.deleteFile(decryptedFile);
		}
	}

	void prependTimestampAndEncryptFile(final @Nonnull File encryptedFile, final @Nonnull File localFile)
			throws IOException {
		try (FileInputStream in = new FileInputStream(localFile);
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(encryptedFile))) {
			DataOutputStream dataOut = new DataOutputStream(out);
			dataOut.writeLong(TimeUnit.MILLISECONDS.toSeconds(localFile.lastModified()));
			fileEncrypter.encryptFile(in, out);
			log.finer(() -> "Encrypted \'" + localFile.getAbsolutePath() + "\' to \'" + encryptedFile.getAbsolutePath()
					+ "\'.");

		}
	}

	void decryptFile(final @Nonnull File localFile, final @Nonnull File encryptedFile) throws IOException {
		try (FileInputStream in = new FileInputStream(encryptedFile);
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(localFile))) {
			fileDecrypter.decryptFile(in, out);
			log.finer(() -> "Decrypted \'" + encryptedFile.getAbsolutePath() + "\' to \'" + localFile.getAbsolutePath()
					+ "\'.");
		}
	}
}
