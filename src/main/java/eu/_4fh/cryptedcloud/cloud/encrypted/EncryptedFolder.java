package eu._4fh.cryptedcloud.cloud.encrypted;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import eu._4fh.cryptedcloud.cloud.AbstractCloudFolder;
import eu._4fh.cryptedcloud.cloud.CloudFile;
import eu._4fh.cryptedcloud.cloud.CloudFolder;
import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.util.Util;

public class EncryptedFolder extends AbstractCloudFolder {
	@SuppressWarnings("unused")
	private static final Logger log = Util.getLogger();
	private final @Nonnull CloudFolder folder;
	private final @Nonnull EncryptedCloud cloud;
	private final @Nonnull Map<String, EncryptedFile> files;
	private final @Nonnull Map<String, EncryptedFolder> folders;

	EncryptedFolder(final @Nonnull EncryptedCloud cloud, final @Nonnull CloudFolder folder) {
		this.cloud = cloud;
		this.folder = folder;
		files = new HashMap<String, EncryptedFile>();
		folders = new HashMap<String, EncryptedFolder>();
		for (CloudFolder cloudFolder : folder.getSubFolders().values()) {
			folders.put(cloud.getNameEncrypter().decryptName(cloudFolder.getFolderName()),
					new EncryptedFolder(cloud, cloudFolder));
		}
		for (CloudFile cloudFile : folder.getFiles().values()) {
			files.put(cloud.getNameEncrypter().decryptName(cloudFile.getFileName()),
					new EncryptedFile(cloud, cloudFile));
		}
	}

	@Override
	public @Nonnull String getFolderName() {
		return cloud.getNameEncrypter().decryptName(folder.getFolderName());
	}

	@Override
	public @Nonnull Map<String, CloudFolder> getSubFolders() {
		return Collections.unmodifiableMap(folders);
	}

	@Override
	public @Nonnull Map<String, CloudFile> getFiles() {
		return Collections.unmodifiableMap(files);
	}

	@Override
	public @Nonnull CloudFile createFile(final @Nonnull String fileName, @Nonnull File file) throws IOException {
		String encryptedName = cloud.getNameEncrypter().encryptName(fileName);
		File encryptedFile = File.createTempFile(file.getName(), ".crypted", Config.getInstance().getTempDir());
		try {
			cloud.prependTimestampAndEncryptFile(encryptedFile, file);
			file = null;
			EncryptedFile newFile = new EncryptedFile(cloud, folder.createFile(encryptedName, encryptedFile));
			files.put(fileName, newFile);
			return newFile;
		} finally {
			Util.deleteFile(encryptedFile);
		}
	}

	@Override
	public @Nonnull CloudFolder createFolder(final @Nonnull String folderName) throws IOException {
		String encryptedName = cloud.getNameEncrypter().encryptName(folderName);
		EncryptedFolder newFolder = new EncryptedFolder(cloud, folder.createFolder(encryptedName));
		folders.put(folderName, newFolder);
		return newFolder;
	}

	@Override
	public @Nonnull boolean deleteFile(final @Nonnull String fileName) throws IOException {
		if (files.containsKey(fileName)) {
			files.remove(fileName);
		}
		return folder.deleteFile(cloud.getNameEncrypter().encryptName(fileName));
	}

	@Override
	public @Nonnull boolean deleteFile(final @Nonnull CloudFile file) throws IOException {
		return deleteFile(file.getFileName());
	}

	@Override
	public @Nonnull boolean deleteFolder(final @Nonnull String folderName) throws IOException {
		if (folders.containsKey(folderName)) {
			folders.remove(folderName);
		}
		return folder.deleteFolder(cloud.getNameEncrypter().encryptName(folderName));
	}

	@Override
	public @Nonnull boolean deleteFolder(final @Nonnull CloudFolder folder) throws IOException {
		return deleteFolder(folder.getFolderName());
	}

	@Nonnull
	CloudFolder getCloudFolder() {
		return folder;
	}

	@Override
	public long getLastModfication() throws IOException {
		return folder.getLastModfication();
	}
}
