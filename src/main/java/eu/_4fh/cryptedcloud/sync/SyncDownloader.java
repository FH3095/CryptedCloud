package eu._4fh.cryptedcloud.sync;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import eu._4fh.cryptedcloud.cloud.CloudFile;
import eu._4fh.cryptedcloud.cloud.CloudFolder;
import eu._4fh.cryptedcloud.cloud.CloudService;
import eu._4fh.cryptedcloud.util.Util;

public class SyncDownloader {
	private static final Logger log = Util.getLogger();
	private final List<File> syncFolderList;
	private final CloudFolder cloudRootFolder;
	private final CloudService cloud;
	private final PrintStream msgStream;

	public SyncDownloader(final @Nonnull PrintStream msgStream, final @Nonnull List<File> syncFolderList,
			final @Nonnull CloudFolder cloudRootFolder, final @Nonnull CloudService cloud) {
		this.msgStream = msgStream;
		this.syncFolderList = syncFolderList;
		this.cloudRootFolder = cloudRootFolder;
		this.cloud = cloud;
	}

	public boolean doSync() throws IOException {
		boolean successfullSync = true;
		Set<String> syncedFolderNames = new HashSet<String>();
		msgStream.println("Starting Download.");
		cloud.startSync(false);
		for (File folder : syncFolderList) {
			String folderName = folder.getAbsolutePath();
			syncedFolderNames.add(folderName);
			CloudFolder cloudFolder;
			if (cloudRootFolder.getSubFolders().containsKey(folderName)) {
				cloudFolder = cloudRootFolder.getSubFolders().get(folderName);
				if (!syncFolder(cloudFolder, folder)) {
					successfullSync = false;
				}
			} else {
				log.info(() -> "Cant download folder \"" + folderName + "\", because doesnt exist in cloud "
						+ cloud.toString() + "!");
				msgStream.println(
						"Can't download folder \"" + folderName + "\" because it doesn't exists in the cloud.");
			}
		}
		Set<String> nonSyncedFolderNames = new HashSet<String>(cloudRootFolder.getSubFolders().keySet());
		nonSyncedFolderNames.removeAll(syncedFolderNames);
		if (!nonSyncedFolderNames.isEmpty()) {
			if (log.isLoggable(Level.INFO)) {
				StringBuffer logMsg = new StringBuffer(
						"Didn't synced following folders, because they are not in the sync list:");
				nonSyncedFolderNames.forEach(folderName -> logMsg.append(folderName).append(", "));
				log.info(logMsg.toString());
			}

			msgStream.print("Found folder in cloud that doesn't exist local or that are not in the sync-list: ");
			for (String folder : nonSyncedFolderNames) {
				msgStream.print(folder);
				msgStream.print(", ");
			}
			msgStream.println();
		}
		cloud.finishSync(false, successfullSync);
		msgStream.println("Download finished.");
		return successfullSync;
	}

	private boolean syncFolder(final @Nonnull CloudFolder cloudFolder, final @Nonnull File folder) {
		boolean successfullSync = true;

		for (Map.Entry<String, CloudFolder> cloudFolderEntry : cloudFolder.getSubFolders().entrySet()) {
			File localFolder = new File(folder, cloudFolderEntry.getKey());
			if (localFolder.exists() && !localFolder.isDirectory()) {
				localFolder.delete();
				log.info(() -> "Deleted local File \"" + localFolder.getAbsolutePath() + "\" to create a folder.");
				msgStream.println(
						"Deleted local File \"" + localFolder.getAbsolutePath() + "\" to create a folder instead.");
			}
			if (!localFolder.exists()) {
				if (!localFolder.mkdir()) {
					log.severe(() -> "Cant create folder \"" + localFolder.getAbsolutePath() + "\"");
					msgStream.println("Failed to create local folder \"" + localFolder.getAbsolutePath() + "\"");
					successfullSync = false;
					continue;
				}
				msgStream.println("Created Folder \"" + localFolder.getAbsolutePath() + "\".");
			}
			syncFolder(cloudFolderEntry.getValue(), localFolder);
		}
		for (Map.Entry<String, CloudFile> cloudFileEntry : cloudFolder.getFiles().entrySet()) {
			File localFile = new File(folder, cloudFileEntry.getKey());
			try {
				if (localFile.exists()) {
					if (localFile.isDirectory()) {
						Files.walkFileTree(localFile.toPath(), new SimpleFileVisitor<Path>() {
							@Override
							public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
								Files.delete(file);
								return FileVisitResult.CONTINUE;
							}

							@Override
							public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
								if (e == null) {
									Files.delete(dir);
									return FileVisitResult.CONTINUE;
								} else {
									// directory iteration failed
									throw e;
								}
							}
						});
						log.info(() -> "Deleted local directory \"" + localFile.getAbsolutePath()
								+ "\" to create a file.");
						msgStream.println("Deleted local directory \"" + localFile.getAbsolutePath()
								+ "\" to create a file instead.");
					} else if (!localFile.isFile()) {
						localFile.delete();
						log.info(() -> "Deleted local \"" + localFile.getAbsolutePath() + "\" to create a file.");
						msgStream.println(
								"Deleted local \"" + localFile.getAbsolutePath() + "\" to create a file instead.");
					}
				}
				if (fileNeedsUpdate(localFile, cloudFileEntry.getValue())) {
					cloudFileEntry.getValue().downloadFile(localFile);
					log.info(() -> "Downloaded file \"" + cloudFileEntry.getKey() + "\" to \""
							+ localFile.getAbsolutePath() + "\"");
					msgStream.println("Downloaded file " + localFile.getAbsolutePath());
				} else {
					log.finer(() -> "File \"" + cloudFileEntry.getKey() + "\" already consistent with \""
							+ localFile.getAbsolutePath() + "\"");
				}
			} catch (IOException e) {
				log.log(Level.SEVERE, "Cant download file \"" + cloudFileEntry.getKey() + "\" to \""
						+ localFile.getAbsolutePath() + "\"", e);
				msgStream.println(
						"Can't download file \"" + localFile.getAbsolutePath() + "\" because " + e.getMessage());
				successfullSync = false;
			}
		}
		return successfullSync;
	}

	private boolean fileNeedsUpdate(final @Nonnull File localFile, final @Nonnull CloudFile cloudFile)
			throws IOException {
		long localLastModified = TimeUnit.MILLISECONDS.toSeconds(localFile.lastModified());
		long remoteLastModified = cloudFile.getLastModification();
		log.finest(() -> "Comparing file timestamps: " + localLastModified
				+ (localLastModified == remoteLastModified ? " = "
						: (localLastModified < remoteLastModified ? " < " : " > "))
				+ remoteLastModified + " for \'" + localFile.getAbsolutePath() + "\' and \'" + cloudFile.getFileName()
				+ "\'.");
		if (remoteLastModified > localLastModified) {
			return true;
		} else {
			return false;
		}
	}
}
