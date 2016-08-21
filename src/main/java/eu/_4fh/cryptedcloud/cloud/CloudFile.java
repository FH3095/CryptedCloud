package eu._4fh.cryptedcloud.cloud;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;

public interface CloudFile {
	public static final int LAST_BYTES_UNDEF_VALUE = -1;

	public @Nonnull String getFileName();

	public void updateFile(@Nonnull final File file) throws IOException;

	public void downloadFile(@Nonnull final File file) throws IOException;

	public void downloadFile(@Nonnull final File file, final int firstByte, final int lastByte) throws IOException;

	public long getLastModification() throws IOException;
}
