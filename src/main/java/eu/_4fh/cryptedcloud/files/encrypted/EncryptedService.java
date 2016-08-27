package eu._4fh.cryptedcloud.files.encrypted;

import java.io.IOException;
import java.util.Collection;

import org.abstractj.kalium.keys.KeyPair;
import org.abstractj.kalium.keys.PublicKey;
import org.eclipse.jdt.annotation.NonNull;

import eu._4fh.cryptedcloud.crypt.FileNameEncrypter;
import eu._4fh.cryptedcloud.files.CloudFile;
import eu._4fh.cryptedcloud.files.CloudFolder;
import eu._4fh.cryptedcloud.files.CloudService;
import eu._4fh.cryptedcloud.util.Util;

public class EncryptedService implements CloudService {
	private static final @NonNull String ENCRYPTED_FOLDER_NAME = "EncFolder";
	private static final @NonNull String NAME_ENCRYPTER_DATABASE_FILENAME = "FileNameEncrypter.keystore";
	final boolean doCompress = true;
	private final @NonNull CloudService service;
	private final @NonNull FileNameEncrypter nameEncrypter;
	private final @NonNull EncryptedFolder rootFolder;
	private final @NonNull Collection<KeyPair> privateKeys;
	private final @NonNull Collection<PublicKey> publicKeys;

	public EncryptedService(final @NonNull CloudService service, final @NonNull Collection<KeyPair> privateKeys,
			final @NonNull Collection<PublicKey> publicKeys) throws IOException {
		this.service = service;
		this.privateKeys = privateKeys;
		this.publicKeys = publicKeys;
		nameEncrypter = new FileNameEncrypter();
		CloudFile nameEncrypterCloudFile = service.getRootFolder().getFiles().get(NAME_ENCRYPTER_DATABASE_FILENAME);
		if (nameEncrypterCloudFile != null) {
			nameEncrypter.initializeFromFile(nameEncrypterCloudFile);
		} else {
			nameEncrypter.initializeClean();
		}
		String encryptedFolderName = nameEncrypter.encryptName(ENCRYPTED_FOLDER_NAME);
		CloudFolder encryptedFolder = service.getRootFolder().getSubFolders().get(encryptedFolderName);
		if (encryptedFolder == null) {
			encryptedFolder = service.getRootFolder().createFolder(encryptedFolderName);
		}
		rootFolder = new EncryptedFolder(this, encryptedFolder);
	}

	@Override
	public @NonNull CloudFolder getRootFolder() {
		return rootFolder;
	}

	@Override
	public @NonNull String getUserName() {
		return service.getUserName();
	}

	@Override
	public Util.Pair<Long, Long> getUsageAndLimit() throws IOException {
		return service.getUsageAndLimit();
	}

	@Override
	public @NonNull Long getFreeSpace() throws IOException {
		return service.getFreeSpace();
	}

	@NonNull
	FileNameEncrypter getNameEncrypter() {
		return nameEncrypter;
	}

	@Override
	public void finishSync(final boolean upload, final boolean wasSuccessfull) throws IOException {
		if (!upload) {
			return;
		}
		CloudFile nameEncrypterCloudFile = service.getRootFolder().getFiles().get(NAME_ENCRYPTER_DATABASE_FILENAME);
		if (nameEncrypterCloudFile == null) {
			nameEncrypterCloudFile = service.getRootFolder().createFile(NAME_ENCRYPTER_DATABASE_FILENAME);
		}
		nameEncrypter.writeToFile(nameEncrypterCloudFile);
	}

	@NonNull
	Collection<PublicKey> getPublicKeys() {
		return publicKeys;
	}

	@NonNull
	Collection<KeyPair> getPrivateKeys() {
		return privateKeys;
	}
}
