package eu._4fh.cryptedcloud.gui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.abstractj.kalium.keys.KeyPair;
import org.abstractj.kalium.keys.PublicKey;
import org.eclipse.jdt.annotation.NonNull;

import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.crypt.KeyStore;
import eu._4fh.cryptedcloud.files.CloudService;
import eu._4fh.cryptedcloud.files.encrypted.EncryptedService;
import eu._4fh.cryptedcloud.files.raw.RawService;
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

	public SyncGUI(final @NonNull DefaultListModel<String> syncFolders, final @NonNull JTextComponent output) {
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
			File folder = new File(this.syncFolders.get(i)).getAbsoluteFile();
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
					@NonNull
					LinkedList<PublicKey> publicKeys = new LinkedList<PublicKey>(
							KeyStore.getInstance().getPublicKeys().values());
					KeyStore.getInstance().getPrivateKeys().values()
							.forEach((KeyPair keyPair) -> publicKeys.add(keyPair.getPublicKey()));
					CloudService cloudService = new EncryptedService(
							new RawService(Config.getInstance().getTargetDir()),
							Util.checkNonNull(KeyStore.getInstance().getPrivateKeys().values()), publicKeys);

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
				} catch (Throwable t) {
					outputStream.println("Cant sync: " + t.getMessage());
					log.log(Level.SEVERE, "Cant sync: ", t);
				} finally {
					syncFinished.set(true);
				}
			}
		};
		new WriteStreamToTextThread(Util.checkNonNull(outputBuffer), Util.checkNonNull(output)).start();
		ret.start();
		return ret;
	}

	private class WriteStreamToTextThread extends Thread {
		private static final int SLEEP_TIME = 25;
		private final JTextComponent output;
		private final ByteArrayOutputStream outputBuffer;

		public WriteStreamToTextThread(final @NonNull ByteArrayOutputStream outputBuffer,
				final @NonNull JTextComponent output) {
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
