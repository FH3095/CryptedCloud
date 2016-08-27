package eu._4fh.cryptedcloud.files;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;

import eu._4fh.cryptedcloud.util.Util;

public interface CloudService {
	public @NonNull CloudFolder getRootFolder();

	public @NonNull String getUserName();

	public Util.Pair<Long, Long> getUsageAndLimit() throws IOException;

	public @NonNull Long getFreeSpace() throws IOException;

	public default void startSync(final boolean upload) throws IOException {
	}

	public default void finishSync(final boolean upload, final boolean wasSuccessfull) throws IOException {
	}
}
