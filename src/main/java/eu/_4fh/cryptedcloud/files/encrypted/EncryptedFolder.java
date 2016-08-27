package eu._4fh.cryptedcloud.files.encrypted;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import eu._4fh.cryptedcloud.files.AbstractCloudFolder;
import eu._4fh.cryptedcloud.files.CloudFile;
import eu._4fh.cryptedcloud.files.CloudFolder;
import eu._4fh.cryptedcloud.util.Util;

public class EncryptedFolder extends AbstractCloudFolder {
	private static final Logger log = Util.getLogger();
	private final @NonNull CloudFolder folder;
	private final @NonNull EncryptedService service;
	private final @NonNull Map<@NonNull String, EncryptedFile> files;
	private final @NonNull Map<@NonNull String, EncryptedFolder> folders;

	EncryptedFolder(final @NonNull EncryptedService cloud, final @NonNull CloudFolder folder) throws IOException {
		this.service = cloud;
		this.folder = folder;
		files = new HashMap<@NonNull String, EncryptedFile>();
		folders = new HashMap<@NonNull String, EncryptedFolder>();
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
	public @NonNull String getFolderName() {
		return service.getNameEncrypter().decryptName(folder.getFolderName());
	}

	@SuppressWarnings("null")
	@Override
	public @NonNull Map<@NonNull String, CloudFolder> getSubFolders() {
		return Collections.unmodifiableMap(folders);
	}

	@SuppressWarnings("null")
	@Override
	public @NonNull Map<@NonNull String, CloudFile> getFiles() {
		return Collections.unmodifiableMap(files);
	}

	@Override
	public @NonNull CloudFile createFile(final @NonNull String fileName) throws IOException {
		String encryptedName = service.getNameEncrypter().encryptName(fileName);
		EncryptedFile newFile = new EncryptedFile(service, folder.createFile(encryptedName));
		files.put(fileName, newFile);
		log.finest(() -> "Created new file " + fileName + " in " + folder.getFolderName() + " with encrypted name "
				+ encryptedName);
		return newFile;
	}

	@Override
	public @NonNull CloudFolder createFolder(final @NonNull String folderName) throws IOException {
		String encryptedName = service.getNameEncrypter().encryptName(folderName);
		EncryptedFolder newFolder = new EncryptedFolder(service, folder.createFolder(encryptedName));
		folders.put(folderName, newFolder);
		log.finest(() -> "Created new folder " + folderName + " in " + folder.getFolderName() + " with encrypted name "
				+ encryptedName);
		return newFolder;
	}

	@Override
	public boolean deleteFile(final @NonNull String fileName) throws IOException {
		if (files.containsKey(fileName)) {
			files.remove(fileName);
		}
		return folder.deleteFile(service.getNameEncrypter().encryptName(fileName));
	}

	@Override
	public boolean deleteFile(final @NonNull CloudFile file) throws IOException {
		return deleteFile(file.getFileName());
	}

	@Override
	public boolean deleteFolder(final @NonNull String folderName) throws IOException {
		if (folders.containsKey(folderName)) {
			folders.remove(folderName);
		}
		return folder.deleteFolder(service.getNameEncrypter().encryptName(folderName));
	}

	@Override
	public boolean deleteFolder(final @NonNull CloudFolder folder) throws IOException {
		return deleteFolder(folder.getFolderName());
	}

	@NonNull
	CloudFolder getCloudFolder() {
		return folder;
	}
}
