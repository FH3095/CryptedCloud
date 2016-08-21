package eu._4fh.cryptedcloud.crypt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.abstractj.kalium.encoders.Encoder;
import org.abstractj.kalium.keys.KeyPair;
import org.abstractj.kalium.keys.PrivateKey;
import org.abstractj.kalium.keys.PublicKey;

import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.util.Util;

public class KeyStore {
	private static final Logger log = Util.getLogger();
	private static final String KEYSTORE_DIR = "/Keys/";
	private static final String KEY_POSTFIX = ".key";
	private static final String PUB_POSTFIX = ".pub";
	private static KeyStore instance = null;
	private final Map<String, PublicKey> publicKeys;
	private final Map<String, PrivateKey> privateKeys;

	private KeyStore() {
		publicKeys = new HashMap<String, PublicKey>();
		privateKeys = new HashMap<String, PrivateKey>();
		File keyDir = new File(Config.getInstance().getConfigDir(), KEYSTORE_DIR);
		if (!keyDir.exists()) {
			keyDir.mkdir();
		}
		try {
			readFromFiles();
		} catch (IOException e) {
			log.log(Level.SEVERE, "Cant read keyfiles: ", e);
			throw new RuntimeException(e);
		}
	}

	static public KeyStore getInstance() {
		if (instance == null) {
			instance = new KeyStore();
		}
		return instance;
	}

	public @Nonnull Map<String, PublicKey> getPublicKeys() {
		return Collections.unmodifiableMap(publicKeys);
	}

	public @Nonnull Map<String, PrivateKey> getPrivateKeys() {
		return Collections.unmodifiableMap(privateKeys);
	}

	public void writeToFile() throws IOException {
		File folder = new File(Config.getInstance().getConfigDir(), KEYSTORE_DIR);
		for (Map.Entry<String, PrivateKey> entry : privateKeys.entrySet()) {
			try (OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(folder, entry.getKey())))) {
				out.write(Encoder.HEX.encode(entry.getValue().toBytes()).getBytes(StandardCharsets.UTF_8));
			}
		}
		for (Map.Entry<String, PublicKey> entry : publicKeys.entrySet()) {
			try (OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(folder, entry.getKey())))) {
				out.write(Encoder.HEX.encode(entry.getValue().toBytes()).getBytes(StandardCharsets.UTF_8));
			}
		}
	}

	private void readFromFiles() throws IOException {
		for (File file : new File(Config.getInstance().getConfigDir(), KEYSTORE_DIR).listFiles()) {
			if (!file.isFile()
					&& !(file.getAbsolutePath().endsWith(".pub") || file.getAbsolutePath().endsWith(".key"))) {
				continue;
			}
			try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
				byte[] key = new byte[(int) file.length()];
				Util.readAsMuchFileAsPossible(in, key);
				if (file.getAbsolutePath().endsWith(".pub")) {
					PublicKey pubKey = new PublicKey(Encoder.HEX.decode(new String(key, StandardCharsets.UTF_8)));
					publicKeys.put(file.getName(), pubKey);
					log.finer(() -> "Read public key " + file.getAbsolutePath());
				} else if (file.getAbsolutePath().endsWith(".key")) {
					PrivateKey privKey = new PrivateKey(Encoder.HEX.decode(new String(key, StandardCharsets.UTF_8)));
					privateKeys.put(file.getName(), privKey);
					log.finer(() -> "Read private key " + file.getAbsolutePath());
				}
			}
		}
	}

	public void createKey(@Nonnull String name) {
		if (!name.endsWith(".key")) {
			name = name + ".key";
		}
		privateKeys.put(name, new KeyPair().getPrivateKey());
	}

	public void importKey(final boolean privateKey, final String password, final @Nonnull File file)
			throws IOException {
		byte[] keyEncoded = new byte[(int) file.length()];
		try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
			Util.readAsMuchFileAsPossible(in, keyEncoded);
		}
		byte[] key = Encoder.HEX.decode(new String(keyEncoded, StandardCharsets.UTF_8));
		if (password != null && !password.trim().equals("")) {
			key = new PasswordBasedEncryption().decrypt(password, key);
		}

		String fileName = file.getName();
		if (fileName.endsWith(".key") || fileName.endsWith(".pub")) {
			fileName = fileName.substring(0, fileName.length() - 4);
		}
		if (privateKey) {
			privateKeys.put(fileName + KEY_POSTFIX, new PrivateKey(key));
		} else {
			publicKeys.put(fileName + PUB_POSTFIX, new PublicKey(key));
		}
	}

	public void deleteKey(final @Nonnull String name) {
		privateKeys.remove(name);
		publicKeys.remove(name);
		File keyFile = new File(new File(Config.getInstance().getConfigDir(), KEYSTORE_DIR), name);
		if (!keyFile.delete()) {
			keyFile.deleteOnExit();
		}
		log.info(() -> "Tried to delete file \"" + keyFile.getAbsolutePath() + "\" because key \"" + name
				+ "\" was deleted.");
	}

	public void exportKey(final boolean privateKey, final @Nonnull String name, final String password,
			final @Nonnull File file) throws IOException {
		byte[] key = null;
		if (name.endsWith(".key")) {
			if (privateKey) {
				key = privateKeys.get(name).toBytes();
			} else {
				key = new KeyPair(privateKeys.get(name).toBytes()).getPublicKey().toBytes();
			}
		} else if (name.endsWith(".pub")) {
			key = publicKeys.get(name).toBytes();
		}
		if (key == null) {
			throw new IllegalArgumentException("Key " + name + " doesnt exist!");
		}

		if (password != null && !password.trim().equals("")) {
			key = new PasswordBasedEncryption().encrypt(password, key);
		}

		String keyEncoded = Encoder.HEX.encode(key);
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
			out.write(keyEncoded.getBytes(StandardCharsets.UTF_8));
		}
	}
}
