package eu._4fh.cryptedcloud.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import eu._4fh.cryptedcloud.files.CloudFile;
import eu._4fh.cryptedcloud.files.CloudFolder;
import eu._4fh.cryptedcloud.files.CloudService;
import eu._4fh.cryptedcloud.util.Util;

public class SyncUploader {
	private static final Logger log = Util.getLogger();
	private final List<File> syncFolderList;
	private final CloudFolder cloudRootFolder;
	private final CloudService cloud;
	private final PrintStream msgStream;

	public SyncUploader(final @NonNull PrintStream msgStream, final @NonNull List<File> syncFolderList,
			final @NonNull CloudFolder cloudRootFolder, final @NonNull CloudService cloud) {
		this.msgStream = msgStream;
		this.syncFolderList = syncFolderList;
		this.cloudRootFolder = cloudRootFolder;
		this.cloud = cloud;
	}

	public boolean doSync() throws IOException {
		boolean successfullSync = true;
		Set<@NonNull String> syncedFolderNames = new HashSet<@NonNull String>();
		msgStream.println("Starting Upload.");
		cloud.startSync(true);
		for (File folder : syncFolderList) {
			@SuppressWarnings("null")
			final @NonNull String folderName = folder.getAbsolutePath();
			syncedFolderNames.add(folderName);
			CloudFolder cloudFolder;
			if (cloudRootFolder.getSubFolders().containsKey(folderName)) {
				cloudFolder = cloudRootFolder.getSubFolders().get(folderName);
			} else {
				cloudFolder = cloudRootFolder.createFolder(folderName);
			}
			if (!syncFolder(folder, Util.checkNonNull(cloudFolder))) {
				successfullSync = false;
			}
		}
		Set<@NonNull String> nonSyncedFolderNames = new HashSet<@NonNull String>(
				cloudRootFolder.getSubFolders().keySet());
		nonSyncedFolderNames.removeAll(syncedFolderNames);
		for (String folder : nonSyncedFolderNames) {
			cloudRootFolder.deleteFolder(folder);
		}
		cloud.finishSync(true, successfullSync);
		msgStream.println("Upload finished.");
		return successfullSync;
	}

	@SuppressWarnings("null")
	private boolean syncFolder(final @NonNull File folder, final @NonNull CloudFolder cloudFolder) {
		// TODO Use threads with executor-service to speed up encryption
		boolean successfullSync = true;
		Set<String> syncedFolders = new HashSet<String>();
		Set<String> syncedFiles = new HashSet<String>();

		for (String localFileOrFolderName : folder.list()) {
			File localFileOrFolder = new File(folder, localFileOrFolderName);
			if (!localFileOrFolder.exists()) {
				log.severe("Got non-existent file or folder to sync: " + localFileOrFolder.getAbsolutePath());
				msgStream.println("Got non-existent file or folder to sync: " + localFileOrFolder.getAbsolutePath());
				successfullSync = false;
				continue;
			}
			try {
				if (localFileOrFolder.isDirectory()) {
					syncedFolders.add(localFileOrFolder.getName());
					if (!cloudFolder.getSubFolders().containsKey(localFileOrFolder.getName())) {
						cloudFolder.createFolder(localFileOrFolder.getName());
						log.finer(() -> "Created new folder " + localFileOrFolder.getName() + " for "
								+ localFileOrFolder.getAbsolutePath());
					}
					syncFolder(localFileOrFolder, cloudFolder.getSubFolders().get(localFileOrFolder.getName()));
					log.finer(() -> "Synced folder " + localFileOrFolder.getName() + " to "
							+ localFileOrFolder.getAbsolutePath());
				} else if (localFileOrFolder.isFile()) {
					syncedFiles.add(localFileOrFolder.getName());
					if (!cloudFolder.getFiles().containsKey(localFileOrFolder.getName())) {
						CloudFile cloudFile = cloudFolder.createFile(localFileOrFolder.getName());
						try (OutputStream out = cloudFile.getOutputStream()) {
							new DataOutputStream(out)
									.writeLong(TimeUnit.MILLISECONDS.toSeconds(localFileOrFolder.lastModified()));
							Util.writeFileToStream(localFileOrFolder, out);
						}
						log.finer(() -> "Created new file " + localFileOrFolder.getName() + " for "
								+ localFileOrFolder.getAbsolutePath());
					} else {
						boolean fileNeedsUpdate;
						try (InputStream in = cloudFolder.getFiles().get(localFileOrFolder.getName())
								.getInputStream()) {
							fileNeedsUpdate = fileNeedsUpdate(localFileOrFolder, in);
						}
						if (fileNeedsUpdate) {
							try (OutputStream out = cloudFolder.getFiles().get(localFileOrFolder.getName())
									.getOutputStream()) {
								new DataOutputStream(out)
										.writeLong(TimeUnit.MILLISECONDS.toSeconds(localFileOrFolder.lastModified()));
								out.flush();
								Util.writeFileToStream(localFileOrFolder, out);
							}
							log.finer(() -> "Synced file " + localFileOrFolder.getName() + " to "
									+ localFileOrFolder.getAbsolutePath());
						} else {
							log.finer(() -> "File " + localFileOrFolder.getName() + " already consistent with "
									+ localFileOrFolder.getAbsolutePath());
						}
					}
				}
			} catch (IOException e) {
				log.log(Level.SEVERE, "Exception while trying to sync file " + localFileOrFolder.getAbsolutePath(), e);
				successfullSync = false;
			}
		}

		Set<String> nonSyncedFolders = new HashSet<String>(cloudFolder.getSubFolders().keySet());
		nonSyncedFolders.removeAll(syncedFolders);
		Set<String> nonSyncedFiles = new HashSet<String>(cloudFolder.getFiles().keySet());
		nonSyncedFiles.removeAll(syncedFiles);

		for (String folderName : nonSyncedFolders) {
			try {
				if (!cloudFolder.deleteFolder(folderName)) {
					log.severe(() -> "Cant delete no longer synced folder " + folderName + " in "
							+ folder.getAbsolutePath());
				} else {
					log.info(() -> "Deleted no-longer-synced folder " + folderName + " in " + folder.getAbsolutePath());
					msgStream.println("Deleted Cloud-Folder for \"" + folderName + "\".");
				}
			} catch (IOException e) {
				log.log(Level.SEVERE,
						"Exception while trying to delete folder " + folderName + " in " + folder.getAbsolutePath(), e);
				successfullSync = false;
			}
		}
		for (String fileName : nonSyncedFiles) {
			try {
				if (!cloudFolder.deleteFile(fileName)) {
					log.severe(
							() -> "Cant delete no longer synced file " + fileName + " in " + folder.getAbsolutePath());
				} else {
					log.finer(() -> "Deleted no-longer-synced file " + fileName + " in " + folder.getAbsolutePath());
				}
			} catch (IOException e) {
				log.log(Level.SEVERE,
						"Exception while trying to delete file " + fileName + " in " + folder.getAbsolutePath(), e);
				successfullSync = false;
			}
		}
		return successfullSync;
	}

	private boolean fileNeedsUpdate(final @NonNull File localFile, final @NonNull InputStream in) throws IOException {
		long localLastModified = TimeUnit.MILLISECONDS.toSeconds(localFile.lastModified());
		long remoteLastModified = new DataInputStream(in).readLong();
		log.finest(() -> "Comparing file timestamps: " + localLastModified
				+ (localLastModified == remoteLastModified ? " = "
						: (localLastModified < remoteLastModified ? " < " : " > "))
				+ remoteLastModified + " for \'" + localFile.getAbsolutePath() + "\'.");
		if (localLastModified > remoteLastModified) {
			return true;
		} else {
			return false;
		}
	}
}
