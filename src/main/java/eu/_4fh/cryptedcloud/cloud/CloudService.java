package eu._4fh.cryptedcloud.cloud;

import java.io.IOException;

import javax.annotation.Nonnull;

import eu._4fh.cryptedcloud.util.Util;

public interface CloudService {
	public @Nonnull CloudFolder getRootFolder();

	public @Nonnull String getUserName();

	public @Nonnull Util.Pair<Long, Long> getUsageAndLimit(boolean refresh) throws IOException;

	public @Nonnull Long getFreeSpace(boolean refresh) throws IOException;

	public default void startSync(final boolean upload) throws IOException {
	}

	public default void finishSync(final boolean upload, final boolean wasSuccessfull) throws IOException {
	}
}
