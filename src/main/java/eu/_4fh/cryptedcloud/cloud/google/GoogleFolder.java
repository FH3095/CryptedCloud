package eu._4fh.cryptedcloud.cloud.google;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.model.File;

import eu._4fh.cryptedcloud.cloud.AbstractCloudFolder;
import eu._4fh.cryptedcloud.cloud.CloudFile;
import eu._4fh.cryptedcloud.cloud.CloudFolder;
import eu._4fh.cryptedcloud.util.Util;

public class GoogleFolder extends AbstractCloudFolder {
	private static final Logger log = Util.getLogger();
	private final File file;
	private final GoogleCloud cloud;
	private Map<String, GoogleFile> files;
	private Map<String, GoogleFolder> folders;

	GoogleFolder(final @Nonnull GoogleCloud cloud, final @Nonnull File file) {
		this.file = file;
		this.cloud = cloud;
		files = new HashMap<String, GoogleFile>();
		folders = new HashMap<String, GoogleFolder>();
	}

	@Override
	public @Nonnull String getFolderName() {
		return file.getName();
	}

	File getFile() {
		return file;
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
	public @Nonnull CloudFile createFile(@Nonnull final String fileName, @Nonnull final java.io.File file)
			throws IOException {
		try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));) {
			InputStreamContent stream = new InputStreamContent("application/octet-stream", inputStream);
			stream.setLength(file.length());
			File googleFile = new File();
			googleFile.setName(fileName);
			googleFile.setParents(Collections.singletonList(this.file.getId()));
			File newFile = cloud.getDrive().files().create(googleFile, stream).setFields(GoogleCloud.FILE_FIELDS)
					.execute();
			GoogleFile newCloudFile = new GoogleFile(cloud, newFile);
			files.put(fileName, newCloudFile);
			log.finer(() -> "Created File " + newCloudFile.getFile().getName() + " (" + newCloudFile.getFile().getId()
					+ ") from " + file.getAbsolutePath());
			return newCloudFile;
		}
	}

	public @Nonnull CloudFolder createFolder(@Nonnull final String folderName) throws IOException {
		File googleFolder = new File();
		googleFolder.setName(folderName);
		googleFolder.setParents(Collections.singletonList(this.file.getId()));
		googleFolder.setMimeType(GoogleCloud.FOLDER_MIME_TYPE);
		File newFolder = cloud.getDrive().files().create(googleFolder).setFields(GoogleCloud.FILE_FIELDS).execute();
		GoogleFolder newCloudFolder = new GoogleFolder(cloud, newFolder);
		folders.put(folderName, newCloudFolder);
		log.finer(() -> "Created Folder " + newCloudFolder.getFile().getName() + " (" + newCloudFolder.getFile().getId()
				+ ")");
		return newCloudFolder;
	}

	@Nonnull
	GoogleFolder addGoogleFolder(@Nonnull File file) {
		if (!file.getMimeType().equalsIgnoreCase(GoogleCloud.FOLDER_MIME_TYPE)) {
			throw new IllegalArgumentException(
					"Expected Folder-Mimetype but got " + file.getMimeType() + " ; " + file.toString());
		}
		if (folders.containsKey(file.getName())) {
			throw new RuntimeException(
					"Tried to add already existing folder: " + file.getName() + " ; " + file.toString());
		}
		GoogleFolder newFolder = new GoogleFolder(cloud, file);
		folders.put(file.getName(), newFolder);
		return newFolder;
	}

	@Nonnull
	GoogleFile addGoogleFile(@Nonnull File file) {
		if (file.getMimeType().equalsIgnoreCase(GoogleCloud.FOLDER_MIME_TYPE)) {
			throw new IllegalArgumentException(
					"Expected File-Mimetype but got " + file.getMimeType() + " ; " + file.toString());
		}
		if (files.containsKey(file.getName())) {
			throw new RuntimeException(
					"Tried to add already exisiting file: " + file.getName() + " ; " + file.toString());
		}
		GoogleFile newFile = new GoogleFile(cloud, file);
		files.put(file.getName(), newFile);
		return newFile;
	}

	@Override
	public @Nonnull String toString() {
		return "[" + file.getName() + ";" + file.toString() + "]";
	}

	@Override
	public boolean deleteFile(final @Nonnull String fileName) throws IOException {
		if (!files.containsKey(fileName)) {
			return false;
		}
		cloud.getDrive().files().delete(files.get(fileName).getFile().getId()).execute();
		files.remove(fileName);
		log.finer(() -> "Deleted File " + fileName);
		return true;
	}

	@Override
	public boolean deleteFile(final @Nonnull CloudFile file) throws IOException {
		return deleteFile(file.getFileName());
	}

	@Override
	public boolean deleteFolder(final @Nonnull String folderName) throws IOException {
		if (!folders.containsKey(folderName)) {
			return false;
		}
		cloud.getDrive().files().delete(folders.get(folderName).getFile().getId()).execute();
		folders.remove(folderName);
		log.finer(() -> "Deleted Folder " + folderName);
		return true;
	}

	@Override
	public boolean deleteFolder(final @Nonnull CloudFolder folder) throws IOException {
		return deleteFolder(folder.getFolderName());
	}

	@Override
	public long getLastModfication() throws IOException {
		long millis = file.getModifiedTime().getValue();
		return TimeUnit.MILLISECONDS.toSeconds(millis);
	}
}
