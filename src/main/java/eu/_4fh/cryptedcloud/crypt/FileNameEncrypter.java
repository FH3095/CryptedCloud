package eu._4fh.cryptedcloud.crypt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;

import org.cryptomator.siv.SivMode;

import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.util.Util;

public class FileNameEncrypter {
	private static final Logger log = Util.getLogger();
	private static final int DEFAULT_KEY_SIZE = 256;
	private static final String DEFAULT_KEY_ALGORITHM = "AES";
	private boolean initialized;
	private final @Nonnull SivMode sivMode;
	private String sivKeyAlgorithm;
	private int sivKeySize;
	private SecretKey sivCtrKey;
	private SecretKey sivMacKey;

	public FileNameEncrypter() {
		sivMode = new SivMode();
		initialized = false;
	}

	public void finalize() {
		try {
			sivCtrKey.destroy();
		} catch (DestroyFailedException e) {
			// Ignore
		}
		try {
			sivMacKey.destroy();
		} catch (DestroyFailedException e) {
			// Ignore
		}
	}

	public synchronized void initializeClean() {
		if (initialized) {
			throw new IllegalStateException("Cant re-initialize FileNameEncrypter!");
		}
		KeyGenerator keyGen;
		sivKeyAlgorithm = DEFAULT_KEY_ALGORITHM;
		sivKeySize = DEFAULT_KEY_SIZE;
		try {
			keyGen = KeyGenerator.getInstance(sivKeyAlgorithm);
			int maxAllowedKeySize = Cipher.getMaxAllowedKeyLength(sivKeyAlgorithm);
			if (Config.getInstance().getAllowWeakNameEncryptionKey() && maxAllowedKeySize < DEFAULT_KEY_SIZE) {
				log.warning(() -> "Using weak " + sivKeyAlgorithm
						+ " key to encrypt filenames! JCE Unlimited Strength Jurisdiction Policy Files should be used! Maximum KeySize is "
						+ maxAllowedKeySize);
				sivKeySize = maxAllowedKeySize;
			}
			keyGen.init(sivKeySize, SecureRandom.getInstanceStrong());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Cant generate key for filename-encryption.", e);
		}
		sivCtrKey = keyGen.generateKey();
		sivMacKey = keyGen.generateKey();
		initialized = true;
	}

	public synchronized void initializeFromFile(final @Nonnull File file) throws IOException {
		if (initialized) {
			throw new IllegalStateException("Cant re-initialize FileNameEncrypter!");
		}
		try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
			int keyLength;
			byte[] tmp;
			sivKeyAlgorithm = in.readUTF();
			sivKeySize = in.readInt();
			if (sivKeySize < DEFAULT_KEY_SIZE) {
				log.warning("The keys that were used to encrypt the file names was weak. The keysize was " + sivKeySize
						+ ".");
				if (!Config.getInstance().getAllowWeakNameEncryptionKey()) {
					throw new RuntimeException("Tried to read weak file encryption key with " + sivKeySize + " from "
							+ file.getAbsolutePath() + ", but Config doesn't allow this!");
				}
			}

			keyLength = in.readInt();
			tmp = new byte[keyLength];
			in.readFully(tmp);
			sivCtrKey = new SecretKeySpec(tmp, sivKeyAlgorithm);

			keyLength = in.readInt();
			tmp = new byte[keyLength];
			in.readFully(tmp);
			sivMacKey = new SecretKeySpec(tmp, sivKeyAlgorithm);

			initialized = true;
		}
	}

	public @Nonnull String encryptName(final @Nonnull String name) {
		checkInitialized();
		byte[] clearFileName = name.getBytes(StandardCharsets.UTF_8);
		byte[] encryptedFileName = sivMode.encrypt(sivCtrKey, sivMacKey, clearFileName);
		return Base64.getUrlEncoder().encodeToString(encryptedFileName);
	}

	public @Nonnull String decryptName(final @Nonnull String idStr) {
		checkInitialized();
		try {
			byte[] encryptedFileName = Base64.getUrlDecoder().decode(idStr);
			byte[] clearFileName = sivMode.decrypt(sivCtrKey, sivMacKey, encryptedFileName);
			return new String(clearFileName, StandardCharsets.UTF_8);
		} catch (AEADBadTagException e) {
			throw new RuntimeException("Cant decrypt encoded filename " + idStr, e);
		}
	}

	public void writeToFile(final @Nonnull File file) throws IOException {
		checkInitialized();
		try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
			byte[] tmp;
			out.writeUTF(sivKeyAlgorithm);
			out.writeInt(sivKeySize);

			tmp = sivCtrKey.getEncoded();
			out.writeInt(tmp.length);
			out.write(tmp);

			tmp = sivMacKey.getEncoded();
			out.writeInt(tmp.length);
			out.write(tmp);
		}
	}

	/*public synchronized void initializeFromFile(final @Nonnull File file) throws IOException {
		if (initialized) {
			throw new IllegalStateException("Cant re-initialize FileNameEncrypter!");
		}
		try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
			int newSize = in.readInt();
			idToName.ensureCapacity(newSize);
			for (int i = 0; i < newSize; ++i) {
				idToName.add(i, null);
			}
			for (int i = 0; i < newSize; ++i) {
				try {
					final int id = in.readInt();
					final String name = in.readUTF();
					reservedNames.set(id);
					idToName.set(id, name);
					nameToId.put(name, id);
				} catch (EOFException e) {
					break;
				}
			}
			initialized = true;
		}
	}
	
	public @Nonnull String getIdForName(final @Nonnull String name) {
		checkInitialized();
		Integer id = nameToId.get(name);
		if (id != null) {
			return Integer.toString(id.intValue(), NAME_TO_NUMBER_RADIX);
		}
		return Integer.toString(createIdForName(name), NAME_TO_NUMBER_RADIX);
	}
	
	public @Nonnull String getNameForId(final @Nonnull String idStr) {
		checkInitialized();
		int id = Integer.parseInt(idStr, NAME_TO_NUMBER_RADIX);
		if (idToName.size() <= id || idToName.get(id) == null) {
			log.severe(() -> "Cant get name for id " + id + ". Returning pseudo-name!");
			return "_DamagedNameDatabase_" + id;
		}
		return idToName.get(id);
	}
	
	public void writeToFile(final @Nonnull File file) throws IOException {
		checkInitialized();
		try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
			out.writeInt(idToName.size());
			for (int i = 0; i < idToName.size(); ++i) {
				final String name = idToName.get(i);
				if (name == null) {
					continue;
				}
				out.writeInt(i);
				out.writeUTF(name);
			}
		}
	}
	
	public void removeUnusedIds(final @Nonnull Set<String> usedIds) {
		checkInitialized();
		Set<Integer> unusedIds = new HashSet<Integer>(nameToId.values());
		for (final String idStr : usedIds) {
			Integer id = Integer.parseInt(idStr, NAME_TO_NUMBER_RADIX);
			unusedIds.remove(id);
		}
		for (final Integer id : unusedIds) {
			removeId(id);
		}
	}
	
	private int createIdForName(final @Nonnull String name) {
		int nextFreeName = reservedNames.nextClearBit(0);
		reservedNames.set(nextFreeName);
	
		if (idToName.size() <= nextFreeName) {
			if (idToName.size() != nextFreeName) {
				StringBuffer buff = new StringBuffer();
				buff.append("idToName should grow but has to grow more than 1 pos. This should not happen! ");
				buff.append("nextPos=").append(nextFreeName).append(" ; idToName.size=").append(idToName.size());
				buff.append("nameBits=");
				for (int i = 0; i <= nextFreeName; ++i) {
					buff.append(reservedNames.get(i));
				}
				buff.append(" ; names=");
				for (String curName : idToName) {
					buff.append(curName).append('\\');
				}
				throw new RuntimeException(buff.toString());
			}
			log.finer(() -> "Ensure NameToId-Capacity is at least "
					+ (Math.floorDiv(idToName.size(), ARRAY_GROW_STEPS) * ARRAY_GROW_STEPS + ARRAY_GROW_STEPS)
					+ " and grow idToName for name " + name);
			idToName.ensureCapacity(
					Math.floorDiv(idToName.size(), ARRAY_GROW_STEPS) * ARRAY_GROW_STEPS + ARRAY_GROW_STEPS);
			idToName.add(name);
		} else {
			idToName.set(nextFreeName, name);
		}
		nameToId.put(name, nextFreeName);
		log.finer(() -> "Created new id " + nextFreeName + " for name " + name);
		return nextFreeName;
	}
	
	private void removeName(final @Nonnull String name) {
		Integer id = nameToId.get(name);
		if (id == null) {
			throw new RuntimeException("No id exists for name " + name);
		}
		removeId(id);
	}
	
	private void removeId(final int id) {
		if (idToName.size() <= id || idToName.get(id) == null) {
			throw new RuntimeException("Id " + id + " is already removed!");
		}
		log.finer(() -> "Removed id " + id + " for name " + idToName.get(id));
		reservedNames.clear(id);
		idToName.set(id, null);
		nameToId.remove(id);
	}*/

	private void checkInitialized() {
		if (!initialized) {
			throw new IllegalStateException("FileNameEncrpyter not initlized!");
		}
	}
}
