package eu._4fh.cryptedcloud.gui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.abstractj.kalium.keys.KeyPair;
import org.abstractj.kalium.keys.PrivateKey;
import org.abstractj.kalium.keys.PublicKey;

import eu._4fh.cryptedcloud.cloud.CloudService;
import eu._4fh.cryptedcloud.cloud.encrypted.EncryptedCloud;
import eu._4fh.cryptedcloud.cloud.google.GoogleCloud;
import eu._4fh.cryptedcloud.crypt.FileDecrypter;
import eu._4fh.cryptedcloud.crypt.FileEncrypter;
import eu._4fh.cryptedcloud.crypt.KeyStore;
import eu._4fh.cryptedcloud.sync.SyncDownloader;
import eu._4fh.cryptedcloud.sync.SyncUploader;
import eu._4fh.cryptedcloud.util.Util;

public class SyncGUI {
	private static final Logger log = Util.getLogger();
	private final JTextComponent output;
	private final DefaultListModel<String> syncFolders;
	private final ByteArrayOutputStream outputBuffer;
	private final AtomicBoolean syncRunning;
	private final AtomicBoolean syncFinished;

	public SyncGUI(final @Nonnull DefaultListModel<String> syncFolders, final @Nonnull JTextComponent output) {
		this.syncFolders = syncFolders;
		this.output = output;
		outputBuffer = new ByteArrayOutputStream();
		syncRunning = new AtomicBoolean(false);
		syncFinished = new AtomicBoolean(false);
	}

	synchronized public Thread doSync(boolean upload) {
		if (!syncRunning.compareAndSet(false, true)) {
			throw new IllegalStateException("Sync is alread running!");
		}
		PrintStream outputStream;
		try {
			outputStream = new PrintStream(outputBuffer, true, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		ArrayList<File> syncFolders = new ArrayList<File>(this.syncFolders.size());
		for (int i = 0; i < this.syncFolders.size(); ++i) {
			File folder = new File(this.syncFolders.get(i));
			if (!folder.exists() || !folder.isDirectory()) {
				log.info(() -> "Cant sync folder \"" + folder.getAbsolutePath()
						+ "\" because it either doesnt exists or is not a folder!");
				outputStream.println(
						"Cant sync folder \"" + folder.getAbsolutePath() + "\": Doesnt exist or not a folder.");
			}
			syncFolders.add(folder);
		}

		Thread ret = new Thread() {
			@Override
			public void run() {
				try {
					FileEncrypter encrypter = new FileEncrypter();
					FileDecrypter decrypter = new FileDecrypter();
					for (PublicKey pubKey : KeyStore.getInstance().getPublicKeys().values()) {
						encrypter.addKey(pubKey);
					}
					for (PrivateKey privKey : KeyStore.getInstance().getPrivateKeys().values()) {
						encrypter.addKey(new KeyPair(privKey.toBytes()).getPublicKey());
						decrypter.addKey(new KeyPair(privKey.toBytes()));
					}
					CloudService cloudService = new EncryptedCloud(new GoogleCloud(), encrypter, decrypter);

					if (upload) {
						if (!new SyncUploader(outputStream, syncFolders, cloudService.getRootFolder(), cloudService)
								.doSync()) {
							outputStream.println("Error while sync. Please check log!");
						}
					} else {
						if (!new SyncDownloader(outputStream, syncFolders, cloudService.getRootFolder(), cloudService)
								.doSync()) {
							outputStream.println("Error while sync. Please check log!");
						}
					}
				} catch (GeneralSecurityException | IOException e) {
					outputStream.println("Cant sync: " + e.getMessage());
				} finally {
					syncFinished.set(true);
				}
			}
		};
		new WriteStreamToTextThread(outputBuffer, output).start();
		ret.start();
		return ret;
	}

	private class WriteStreamToTextThread extends Thread {
		private static final int SLEEP_TIME = 25;
		private final JTextComponent output;
		private final ByteArrayOutputStream outputBuffer;

		public WriteStreamToTextThread(final @Nonnull ByteArrayOutputStream outputBuffer,
				final @Nonnull JTextComponent output) {
			this.outputBuffer = outputBuffer;
			this.output = output;
		}

		@Override
		public void run() {
			boolean syncWasFinished = false;
			while (!syncWasFinished) {
				syncWasFinished = syncFinished.get();
				try {
					Thread.sleep(SLEEP_TIME);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							try {
								output.setText(new String(outputBuffer.toByteArray(), StandardCharsets.UTF_8.name()));
							} catch (UnsupportedEncodingException e) {
								// Ignore
							}
						}
					});
				} catch (Throwable t) {
					// Ignore
				}
			}
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					output.setText(output.getText() + "---");
				}
			});
		}
	}
}
