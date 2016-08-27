package eu._4fh.cryptedcloud.files.raw;

import java.io.File;
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

public class RawFolder extends AbstractCloudFolder implements CloudFolder {
	private static final Logger log = Util.getLogger();
	private final @NonNull RawService service;
	private final @NonNull File folder;
	private final @NonNull Map<@NonNull String, @NonNull RawFolder> folders;
	private final @NonNull Map<@NonNull String, @NonNull RawFile> files;

	public RawFolder(final @NonNull RawService service, @NonNull File folder) {
		if (!folder.isDirectory()) {
			throw new IllegalArgumentException("Exptected \"" + folder.getAbsolutePath() + "\" to be a folder!");
		}
		this.service = service;
		this.folder = folder;
		folders = new HashMap<@NonNull String, @NonNull RawFolder>();
		files = new HashMap<@NonNull String, @NonNull RawFile>();
		for (File fileOrFolder : folder.listFiles()) {
			if (fileOrFolder.isDirectory()) {
				folders.put(Util.checkNonNull(fileOrFolder.getName()), new RawFolder(service, fileOrFolder));
			} else {
				files.put(Util.checkNonNull(fileOrFolder.getName()), new RawFile(service, fileOrFolder));
			}
		}
	}

	@SuppressWarnings("null")
	@Override
	public @NonNull String getFolderName() {
		return folder.getName();
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
	public @NonNull CloudFile createFile(@NonNull String fileName) throws IOException {
		File newFile = new File(folder, fileName);
		if (!newFile.createNewFile()) {
			log.severe(() -> "Cant create file " + fileName + " in folder \"" + folder.getAbsolutePath() + "\".");
			throw new IOException("Cant create file " + fileName + " in folder \"" + folder.getAbsolutePath() + "\".");
		}
		RawFile newRawFile = new RawFile(service, newFile);
		files.put(fileName, newRawFile);
		return newRawFile;
	}

	@Override
	public @NonNull CloudFolder createFolder(@NonNull String folderName) throws IOException {
		File newFolder = new File(folder, folderName);
		if (!newFolder.mkdir()) {
			log.severe(() -> "Cant create folder " + folderName + " in folder \"" + folder.getAbsolutePath() + "\".");
			throw new IOException(
					"Cant create folder " + folderName + " in folder \"" + folder.getAbsolutePath() + "\".");
		}
		RawFolder newRawFolder = new RawFolder(service, newFolder);
		folders.put(folderName, newRawFolder);
		return newRawFolder;
	}

	@Override
	public boolean deleteFile(final @NonNull String fileName) throws IOException {
		return deleteFile(files.get(fileName));
	}

	@Override
	public boolean deleteFile(final @NonNull CloudFile file) {
		if (!(file instanceof RawFile)) {
			throw new IllegalArgumentException("RawFolder can only delete RawFiles, but got "
					+ file.getClass().getName() + "(" + file.getFileName() + ")");
		}
		File javaFile = ((RawFile) file).getFile();
		Util.deleteFile(javaFile);
		return true;
	}

	@Override
	public boolean deleteFolder(final @NonNull String folderName) throws IOException {
		return deleteFolder(folders.get(folderName));
	}

	@Override
	public boolean deleteFolder(final @NonNull CloudFolder folder) throws IOException {
		if (!(folder instanceof RawFolder)) {
			throw new IllegalArgumentException("RawFolder can only delete RawFolders, but got "
					+ folder.getClass().getName() + "(" + folder.getFolderName() + ")");
		}
		File javaFolder = ((RawFolder) folder).getFolder();
		Util.deleteFolder(javaFolder);
		return true;
	}

	@NonNull
	File getFolder() {
		return folder;
	}
}
