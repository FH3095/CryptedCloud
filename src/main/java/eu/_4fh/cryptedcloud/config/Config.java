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

import javax.annotation.Nonnull;

import eu._4fh.cryptedcloud.util.Util;

public class Config {
	static public class WritableConfig extends Config {
		private WritableConfig(final @Nonnull Config orig) {
			super(orig);
		}

		public void setFileChunkSize(final int fileChunkSize) {
			this.fileChunkSize = fileChunkSize;
		}

		public void setConfigDir(final @Nonnull File configDir) {
			this.configDir = configDir.getAbsolutePath();
		}

		public void setTempDir(final @Nonnull File tempDir) {
			this.tempDir = tempDir.getAbsolutePath();
		}

		public void setAllowWeakNameEncryptionKey(final boolean allowWeakNameEncryptionKey) {
			this.allowWeakNameEncryptionKey = allowWeakNameEncryptionKey;
		}

		public void setGoogleConfig(final @Nonnull GoogleConfig googleConfig) {
			this.googleConfig = googleConfig;
		}

		public void setSftpConfig(final @Nonnull SftpConfig sftpConfig) {
			this.sftpConfig = sftpConfig;
		}
	}

	private static final Logger log = Util.getLogger();
	private static final String CONFIG_FILE = "CryptedCloudSettings.dat";
	static final String CONFIG_FILE_NAME = "main";
	private static Config instance = null;
	protected int fileChunkSize;
	protected String configDir;
	protected String tempDir;
	protected boolean allowWeakNameEncryptionKey;
	protected GoogleConfig googleConfig;
	protected SftpConfig sftpConfig;

	private Config() {
		fileChunkSize = 100 * 1024 * 1024;
		configDir = System.getProperty("user.home") + "/Desktop/CryptedCloud";
		tempDir = System.getProperty("java.io.tmpdir");
		allowWeakNameEncryptionKey = true;
		googleConfig = new GoogleConfig();
		sftpConfig = new SftpConfig();
	}

	private Config(final @Nonnull Config orig) {
		this.fileChunkSize = orig.fileChunkSize;
		this.configDir = orig.configDir;
		this.tempDir = orig.tempDir;
		this.allowWeakNameEncryptionKey = orig.allowWeakNameEncryptionKey;
		this.googleConfig = orig.googleConfig.clone();
		this.sftpConfig = orig.sftpConfig.clone();
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
					String type;
					try {
						type = in.readUTF();
					} catch (EOFException e) {
						break;
					}
					int version = in.readInt();
					switch (type) {
					case Config.CONFIG_FILE_NAME:
						instance.readV1(in);
						break;
					case GoogleConfig.CONFIG_FILE_NAME:
						instance.googleConfig.readFromFile(version, in);
						break;
					case SftpConfig.CONFIG_FILE_NAME:
						instance.sftpConfig.readFromFile(version, in);
						break;
					default:
						log.severe(() -> "Cant read config, got unexcepted type: " + type + " version " + version);
						throw new RuntimeException("Cant read config, config-file seems to be invalid!");
					}
				}
			}
		}
	}

	private void readV1(final @Nonnull DataInputStream in) throws IOException {
		fileChunkSize = in.readInt();
		configDir = in.readUTF();
		tempDir = in.readUTF();
		allowWeakNameEncryptionKey = in.readBoolean();
	}

	static public void writeAndReloadConfig(final @Nonnull WritableConfig newConfig) throws IOException {
		synchronized (Config.class) {
			try (DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(CONFIG_FILE)))) {
				out.writeUTF("main");
				out.writeInt(1);
				out.writeInt(newConfig.fileChunkSize);
				out.writeUTF(newConfig.configDir);
				out.writeUTF(newConfig.tempDir);
				out.writeBoolean(newConfig.allowWeakNameEncryptionKey);
				newConfig.googleConfig.writeToFile(out);
				newConfig.sftpConfig.writeToFile(out);
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

	public @Nonnull WritableConfig getWritableConfig() {
		return new WritableConfig(this);
	}

	public int getFileChunkSize() {
		return fileChunkSize;
	}

	public @Nonnull File getConfigDir() {
		return new File(configDir);
	}

	public @Nonnull File getTempDir() {
		return new File(tempDir);
	}

	public boolean getAllowWeakNameEncryptionKey() {
		return allowWeakNameEncryptionKey;
	}

	public GoogleConfig getGoogleConfig() {
		return googleConfig;
	}

	public SftpConfig getSftpConfig() {
		return sftpConfig;
	}
}
