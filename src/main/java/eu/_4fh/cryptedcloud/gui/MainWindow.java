package eu._4fh.cryptedcloud.gui;

import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.abstractj.kalium.NaCl;
import org.abstractj.kalium.keys.KeyPair;
import org.abstractj.kalium.keys.PublicKey;
import org.eclipse.jdt.annotation.NonNull;

import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.crypt.FileDecrypter;
import eu._4fh.cryptedcloud.crypt.FileEncrypter;
import eu._4fh.cryptedcloud.crypt.KeyStore;
import eu._4fh.cryptedcloud.util.LogConfigRefresh;
import eu._4fh.cryptedcloud.util.Util;

public class MainWindow extends javax.swing.JFrame {
	private static final long serialVersionUID = -8919560872094579651L;
	private static final Logger log = Util.getLogger();
	private javax.swing.JButton buttonStartDecryption;
	private javax.swing.JButton buttonStartEncryption;
	private javax.swing.JMenuBar menuBarMain;
	private javax.swing.JMenuItem menuItemManageKeys;
	private javax.swing.JMenu menuManage;
	private javax.swing.JPanel panelSyncButtons;
	private javax.swing.JMenuItem menuItemManageConfig;

	public MainWindow() {
		initComponents();
	}

	private void initComponents() {

		panelSyncButtons = new javax.swing.JPanel();
		buttonStartEncryption = new javax.swing.JButton();
		buttonStartDecryption = new javax.swing.JButton();
		menuBarMain = new javax.swing.JMenuBar();
		menuManage = new javax.swing.JMenu();
		menuItemManageKeys = new javax.swing.JMenuItem();
		menuItemManageConfig = new javax.swing.JMenuItem();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("CryptedCloud");
		setLocation(new java.awt.Point(20, 20));
		setPreferredSize(new java.awt.Dimension(300, 75));
		getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.PAGE_AXIS));

		panelSyncButtons.setLayout(new javax.swing.BoxLayout(panelSyncButtons, javax.swing.BoxLayout.LINE_AXIS));

		buttonStartEncryption.setText("Start Encryption");
		buttonStartEncryption.setMaximumSize(new java.awt.Dimension(120, 30));
		buttonStartEncryption.setMinimumSize(new java.awt.Dimension(120, 30));
		buttonStartEncryption.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonStartEncryptionActionPerformed(evt);
			}
		});
		panelSyncButtons.add(buttonStartEncryption);

		buttonStartDecryption.setText("Start Decryption");
		buttonStartDecryption.setMaximumSize(new java.awt.Dimension(120, 30));
		buttonStartDecryption.setMinimumSize(new java.awt.Dimension(120, 30));
		buttonStartDecryption.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonStartDecryptionActionPerformed(evt);
			}
		});
		panelSyncButtons.add(buttonStartDecryption);

		getContentPane().add(panelSyncButtons);

		menuManage.setText("Manage");

		menuItemManageConfig.setText("Config");
		menuItemManageConfig.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				menuItemManageConfigActionPerformed(evt);
			}
		});
		menuManage.add(menuItemManageConfig);
		menuItemManageKeys.setText("Keys");
		menuItemManageKeys.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				menuItemManageKeysActionPerformned(evt);
			}
		});
		menuManage.add(menuItemManageKeys);

		menuBarMain.add(menuManage);

		setJMenuBar(menuBarMain);

		pack();
	}

	private boolean checkSourceAndDestFile(final @NonNull File srcFile, final @NonNull File dstFile) {
		if (!srcFile.exists() || !srcFile.canRead()) {
			JOptionPane.showMessageDialog(this, "Cant read input file: " + srcFile.getAbsolutePath(), "ERROR",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if (dstFile.exists() && !dstFile.canWrite()) {
			JOptionPane.showMessageDialog(this, "Cant write output file: " + dstFile.getAbsolutePath(), "ERROR",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if (dstFile.exists()) {
			int answer = JOptionPane.showConfirmDialog(this,
					"Destination file " + dstFile.getAbsolutePath() + " already exists. Overwrite?", "WARNING",
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (answer != JOptionPane.YES_OPTION) {
				return false;
			}
			if (!dstFile.delete()) {
				JOptionPane.showMessageDialog(null,
						"Cant delete existing destination file " + dstFile.getAbsolutePath(), "ERROR",
						JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		return true;
	}

	private void buttonStartEncryptionActionPerformed(ActionEvent evt) {
		final JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		final @NonNull File srcFile = Util.checkNonNull(fc.getSelectedFile());
		final @NonNull File dstFile = new File(srcFile.getAbsolutePath() + ".enc");

		if (!checkSourceAndDestFile(srcFile, dstFile)) {
			return;
		}

		final @NonNull List<PublicKey> tmpList = new LinkedList<PublicKey>(
				KeyStore.getInstance().getPublicKeys().values());
		KeyStore.getInstance().getPrivateKeys().values()
				.forEach((KeyPair keyPair) -> tmpList.add(keyPair.getPublicKey()));
		final @NonNull List<PublicKey> publicKeys = Util.checkNonNull(Collections.unmodifiableList(tmpList));

		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					try (final FileEncrypter fileEncrypter = new FileEncrypter(
							new BufferedOutputStream(new FileOutputStream(dstFile)), publicKeys)) {
						Util.writeFileToStream(srcFile, fileEncrypter);
					}
				} catch (Throwable t) {
					JOptionPane.showMessageDialog(null, "Error while encrypting file: " + t.getLocalizedMessage(),
							"ERROR", JOptionPane.ERROR_MESSAGE);
					return;
				}
				JOptionPane.showMessageDialog(null,
						"Encrypted \"" + srcFile.getAbsolutePath() + "\" to \"" + dstFile.getAbsolutePath() + "\".",
						"SUCCESS", JOptionPane.INFORMATION_MESSAGE);
			}
		};
		t.start();
	}

	private void buttonStartDecryptionActionPerformed(ActionEvent evt) {
		final JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		final @NonNull File srcFile = Util.checkNonNull(fc.getSelectedFile());
		String dstFilePath = srcFile.getAbsolutePath();
		if (dstFilePath.endsWith(".enc")) {
			dstFilePath = dstFilePath.substring(0, dstFilePath.length() - 4);
		} else {
			dstFilePath = dstFilePath + ".dec";
		}
		final @NonNull File dstFile = new File(dstFilePath);

		if (!checkSourceAndDestFile(srcFile, dstFile)) {
			return;
		}

		final @NonNull List<KeyPair> privateKeys = Util.checkNonNull(Collections
				.unmodifiableList(new LinkedList<KeyPair>(KeyStore.getInstance().getPrivateKeys().values())));

		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					try (final FileDecrypter fileDecrypter = new FileDecrypter(
							new BufferedInputStream(new FileInputStream(srcFile)), privateKeys)) {
						Util.writeStreamToFile(fileDecrypter, dstFile);
					}
				} catch (Throwable t) {
					JOptionPane.showMessageDialog(null, "Error while decrypting file: " + t.getLocalizedMessage(),
							"ERROR", JOptionPane.ERROR_MESSAGE);
					return;
				}
				JOptionPane.showMessageDialog(null,
						"Decrypted \"" + srcFile.getAbsolutePath() + "\" to \"" + dstFile.getAbsolutePath() + "\".",
						"SUCCESS", JOptionPane.INFORMATION_MESSAGE);
			}
		};
		t.start();
	}

	private void menuItemManageConfigActionPerformed(ActionEvent evt) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new ConfigWindow().setVisible(true);
			}
		});
	}

	private void menuItemManageKeysActionPerformned(ActionEvent evt) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new ManageKeysWindow().setVisible(true);
			}
		});
	}

	public static void main(String args[]) {
		/* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
		 * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
		 */
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Windows".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		}
		new LogConfigRefresh();
		try {
			Config.readConfig();
		} catch (IOException e) {
			log.log(Level.SEVERE, "Cant read Config: ", e);
			JOptionPane.showMessageDialog(null, "Can't read config: " + e.getMessage(), "ERROR",
					JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		NaCl.init();
		log.info("System initialized");

		/* Create and display the form */
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new MainWindow().setVisible(true);
			}
		});
	}
}
