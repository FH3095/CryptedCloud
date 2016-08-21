package eu._4fh.cryptedcloud.config;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;

public interface SubConfig extends Cloneable {
	public SubConfig clone();

	public void writeToFile(final @Nonnull DataOutputStream out) throws IOException;

	public void readFromFile(final int version, final @Nonnull DataInputStream in) throws IOException;

	default public @Nonnull File getDataStoreDir() {
		File dataStoreDir = new File(Config.getInstance().getConfigDir(), getClass().getSimpleName());
		if (!dataStoreDir.exists()) {
			dataStoreDir.mkdir();
		}
		return dataStoreDir;
	}

}
