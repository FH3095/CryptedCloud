package eu._4fh.cryptedcloud.cloud;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;

public interface CloudFolder {
	public @Nonnull String getFolderName();

	public @Nonnull Map<String, CloudFolder> getSubFolders();

	public @Nonnull Map<String, CloudFile> getFiles();

	public @Nonnull CloudFile createFile(final @Nonnull String fileName, @Nonnull final File file) throws IOException;

	public @Nonnull CloudFolder createFolder(final @Nonnull String folderName) throws IOException;

	public boolean deleteFile(final @Nonnull String fileName) throws IOException;

	public boolean deleteFile(final @Nonnull CloudFile file) throws IOException;

	public boolean deleteFolder(final @Nonnull String folderName) throws IOException;

	public boolean deleteFolder(final @Nonnull CloudFolder folder) throws IOException;

	public @Nonnull String toString();

	public @Nonnull String toStringRecursive();

	public long getLastModfication() throws IOException;
}
