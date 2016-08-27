package eu._4fh.cryptedcloud.files;

import java.io.IOException;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

public interface CloudFolder {
	public @NonNull String getFolderName();

	public @NonNull Map<@NonNull String, CloudFolder> getSubFolders();

	public @NonNull Map<@NonNull String, CloudFile> getFiles();

	public @NonNull CloudFile createFile(final @NonNull String fileName) throws IOException;

	public @NonNull CloudFolder createFolder(final @NonNull String folderName) throws IOException;

	public boolean deleteFile(final @NonNull String fileName) throws IOException;

	public boolean deleteFile(final @NonNull CloudFile file) throws IOException;

	public boolean deleteFolder(final @NonNull String folderName) throws IOException;

	public boolean deleteFolder(final @NonNull CloudFolder folder) throws IOException;

	public String toString();

	public @NonNull String toStringRecursive();
}
