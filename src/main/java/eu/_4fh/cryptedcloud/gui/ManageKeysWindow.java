package eu._4fh.cryptedcloud.gui;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;

import org.eclipse.jdt.annotation.NonNull;

import eu._4fh.cryptedcloud.crypt.KeyStore;
import eu._4fh.cryptedcloud.util.Util;

public class ManageKeysWindow extends javax.swing.JFrame {

	private static final long serialVersionUID = 3611102412869557724L;
	private static final Logger log = Util.getLogger();
	private final DefaultListModel<String> keysListModel;
	private javax.swing.JButton buttonAddPrivateKey;
	private javax.swing.JButton buttonAddPublicKey;
	private javax.swing.JButton buttonCreateKey;
	private javax.swing.JButton buttonExportKey;
	private javax.swing.JButton buttonRemoveKey;
	private javax.swing.JList<String> listKeys;
	private javax.swing.JPanel panelButtons;
	private javax.swing.JScrollPane scrollPaneKeys;

	public ManageKeysWindow() {
		keysListModel = new DefaultListModel<String>();
		recreateListModel();
		initComponents();
	}

	private void initComponents() {

		scrollPaneKeys = new javax.swing.JScrollPane();
		listKeys = new javax.swing.JList<String>();
		panelButtons = new javax.swing.JPanel();
		buttonCreateKey = new javax.swing.JButton();
		buttonAddPrivateKey = new javax.swing.JButton();
		buttonAddPublicKey = new javax.swing.JButton();
		buttonExportKey = new javax.swing.JButton();
		buttonRemoveKey = new javax.swing.JButton();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("Manage Keys");
		getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.PAGE_AXIS));

		listKeys.setModel(keysListModel);
		listKeys.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPaneKeys.setViewportView(listKeys);

		getContentPane().add(scrollPaneKeys);

		panelButtons.setLayout(new javax.swing.BoxLayout(panelButtons, javax.swing.BoxLayout.LINE_AXIS));

		buttonCreateKey.setText("Create Key");
		buttonCreateKey.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonCreateKeyActionPerformed(evt);
			}
		});
		panelButtons.add(buttonCreateKey);

		buttonAddPrivateKey.setText("Add Private Key");
		buttonAddPrivateKey.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonAddPrivateKeyActionPerformed(evt);
			}
		});
		panelButtons.add(buttonAddPrivateKey);

		buttonAddPublicKey.setText("Add Public Key");
		buttonAddPublicKey.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonAddPublicKeyActionPerformed(evt);
			}
		});
		panelButtons.add(buttonAddPublicKey);

		buttonExportKey.setText("Export Key");
		buttonExportKey.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonExportKeyActionPerformed(evt);
			}
		});
		panelButtons.add(buttonExportKey);

		buttonRemoveKey.setText("Delete Key");
		buttonRemoveKey.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonRemoveKeyActionPerformed(evt);
			}
		});
		panelButtons.add(buttonRemoveKey);

		getContentPane().add(panelButtons);

		pack();
	}

	private void buttonRemoveKeyActionPerformed(ActionEvent evt) {
		final int selected = listKeys.getSelectedIndex();
		if (selected < 0 || selected >= keysListModel.size()) {
			return;
		}
		final @NonNull String keyName = Util.checkNonNull(keysListModel.get(selected));
		final int reallyDelete = JOptionPane.showConfirmDialog(this,
				"Are you sure you want to delete the key " + keyName + "?", "Delete Key", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);
		if (reallyDelete == JOptionPane.YES_OPTION) {
			KeyStore.getInstance().deleteKey(keyName);
		}
		recreateListModel();
	}

	private void buttonExportKeyActionPerformed(ActionEvent evt) {
		final int selected = listKeys.getSelectedIndex();
		if (selected < 0 || selected >= keysListModel.size()) {
			return;
		}
		final @NonNull String keyName = Util.checkNonNull(keysListModel.get(selected));
		final boolean privateKey = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, "Export private key?",
				"Export Key", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		String password = JOptionPane.showInputDialog(this, "Password for key? (Leave empty for no password.)",
				"Password for key", JOptionPane.QUESTION_MESSAGE);
		if (password != null && password.trim().equals("")) {
			password = null;
		}

		final JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		final @NonNull File file = Util.checkNonNull(fc.getSelectedFile());
		try {
			KeyStore.getInstance().exportKey(privateKey, keyName, password, file);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Cant export key " + keyName + " to file \"" + file.getAbsolutePath() + "\": ", e);
			JOptionPane.showMessageDialog(this, "Can't export key: " + e.getMessage(), "ERROR",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void buttonAddPublicKeyActionPerformed(ActionEvent evt) {
		addKeyFromFile(false);
	}

	private void buttonAddPrivateKeyActionPerformed(ActionEvent evt) {
		if (!confirmSecondPrivateKeyIsOk()) {
			return;
		}
		addKeyFromFile(true);
	}

	private void addKeyFromFile(final boolean privateKey) {
		final JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		final @NonNull File file = Util.checkNonNull(fc.getSelectedFile());
		String password = JOptionPane.showInputDialog(this, "Password for the key? (Leave empty for no password.)",
				"Password for key", JOptionPane.QUESTION_MESSAGE);
		if (password != null && password.trim().equals("")) {
			password = null;
		}

		try {
			KeyStore.getInstance().importKey(privateKey, password, file);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Cant add key: ", e);
			JOptionPane.showMessageDialog(this, "Can't import key: " + e.getMessage(), "ERROR",
					JOptionPane.ERROR_MESSAGE);
		}
		recreateListModel();
		saveKeyStore();
	}

	private void buttonCreateKeyActionPerformed(ActionEvent evt) {
		if (!confirmSecondPrivateKeyIsOk()) {
			return;
		}
		final String input = JOptionPane.showInputDialog(this, "Name of the new key?", "New Key",
				JOptionPane.QUESTION_MESSAGE);
		if (input != null && !input.trim().equals("")) {
			KeyStore.getInstance().createKey(input);
			recreateListModel();
			saveKeyStore();
		}
	}

	private void recreateListModel() {
		keysListModel.clear();
		final KeyStore keyStore = KeyStore.getInstance();
		for (String keyName : keyStore.getPrivateKeys().keySet()) {
			keysListModel.addElement(keyName);
		}
		for (String keyName : keyStore.getPublicKeys().keySet()) {
			keysListModel.addElement(keyName);
		}
	}

	private void saveKeyStore() {
		try {
			KeyStore.getInstance().writeToFile();
		} catch (IOException e) {
			log.log(Level.SEVERE, "Cant write keys to config directory: ", e);
			JOptionPane.showMessageDialog(this, "Cant write keys to config directory: " + e.getMessage(), "ERROR",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private boolean confirmSecondPrivateKeyIsOk() {
		if (KeyStore.getInstance().getPrivateKeys().size() < 1
				|| JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
						"A second private key can be problematic. It is not guaranteed, which key is used for encryption. Do you still wand to add another private key?",
						"More than 1 private key?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
			return true;
		}
		return false;
	}
}
