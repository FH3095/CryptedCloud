package eu._4fh.cryptedcloud.config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import eu._4fh.cryptedcloud.util.Util;

public class Config {
	static public class WritableConfig extends Config {
		private WritableConfig(final @NonNull Config orig) {
			super(orig);
		}

		public void setFileChunkSize(final int fileChunkSize) {
			this.fileChunkSize = fileChunkSize;
		}

		public void setConfigDir(final @NonNull File configDir) {
			this.configDir = Util.checkNonNull(configDir.getAbsolutePath());
		}

		public void setTempDir(final @NonNull File tempDir) {
			this.tempDir = Util.checkNonNull(tempDir.getAbsolutePath());
		}
	}

	private static final Logger log = Util.getLogger();
	private static final String CONFIG_FILE = "CryptedCloudSettings.dat";
	private static Config instance = null;
	protected int fileChunkSize;
	protected @NonNull String configDir;
	protected @NonNull String tempDir;

	private Config() {
		fileChunkSize = 100 * 1024 * 1024;
		configDir = System.getProperty("user.home") + "/Desktop/CryptedCloud";
		tempDir = Util.checkNonNull(System.getProperty("java.io.tmpdir"));
	}

	private Config(final @NonNull Config orig) {
		this.fileChunkSize = orig.fileChunkSize;
		this.configDir = orig.configDir;
		this.tempDir = orig.tempDir;
	}

	static public void readConfig() throws IOException {
		synchronized (Config.class) {
			if (instance != null) {
				throw new IllegalStateException("Config already initialized!");
			}
			instance = new Config();
			File configFile = new File(CONFIG_FILE);
			if (!configFile.exists() || !configFile.isFile()) {
				return;
			}
			try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(CONFIG_FILE)))) {
				while (true) {
					String module;
					try {
						module = in.readUTF();
					} catch (EOFException e) {
						break;
					}
					if (!module.equals("main")) {
						throw new UnsupportedOperationException("Module " + module + " unknown!");
					}
					int version = in.readInt();
					switch (version) {
					case 1:
						instance.readV1(in);
						break;
					default:
						log.severe(() -> "Cant read config, got unexcepted version:" + version);
						throw new RuntimeException("Cant read config, config-file seems to be invalid!");
					}
				}
			}
		}
	}

	@SuppressWarnings("null")
	private void readV1(final @NonNull DataInputStream in) throws IOException {
		fileChunkSize = in.readInt();
		configDir = in.readUTF();
		tempDir = in.readUTF();
	}

	static public void writeAndReloadConfig(final @NonNull WritableConfig newConfig) throws IOException {
		synchronized (Config.class) {
			try (DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(CONFIG_FILE)))) {
				out.writeUTF("main");
				out.writeInt(1);
				out.writeInt(newConfig.fileChunkSize);
				out.writeUTF(newConfig.configDir);
				out.writeUTF(newConfig.tempDir);
			}
			instance = null;
			readConfig();
		}
	}

	static public Config getInstance() {
		synchronized (Config.class) {
			if (instance == null) {
				throw new IllegalStateException("Config not yet initialized!");
			}
			return instance;
		}
	}

	public @NonNull WritableConfig getWritableConfig() {
		return new WritableConfig(this);
	}

	public int getFileChunkSize() {
		return fileChunkSize;
	}

	public @NonNull File getConfigDir() {
		return new File(configDir);
	}

	public @NonNull File getTempDir() {
		return new File(tempDir);
	}
}
