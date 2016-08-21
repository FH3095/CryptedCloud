package eu._4fh.cryptedcloud.gui;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.abstractj.kalium.NaCl;

import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.util.LogConfigRefresh;
import eu._4fh.cryptedcloud.util.Util;

public class MainWindow extends javax.swing.JFrame {
	private static final long serialVersionUID = -8919560872094579651L;
	private static final Logger log = Util.getLogger();
	private final DefaultListModel<String> syncFolderListModel;
	private javax.swing.JButton buttonAddSyncedFolder;
	private javax.swing.JButton buttonRemoveSyncedFolder;
	private javax.swing.JButton buttonStartDownload;
	private javax.swing.JButton buttonStartUpload;
	private javax.swing.JList<String> listSyncedFolders;
	private javax.swing.JMenuBar menuBarMain;
	private javax.swing.JMenuItem menuItemManageKeys;
	private javax.swing.JMenu menuManage;
	private javax.swing.JPanel panelSyncButtons;
	private javax.swing.JPanel panelSyncedFolders;
	private javax.swing.JPanel panelSyncedFoldersButtons;
	private javax.swing.JScrollPane scrollPaneSyncStatus;
	private javax.swing.JScrollPane scrollPaneSyncedFolders;
	private javax.swing.JTextPane textPaneSyncStatus;
	private javax.swing.JMenuItem menuItemManageConfig;

	public MainWindow() {
		syncFolderListModel = new DefaultListModel<String>();
		recreateSyncedFoldersList();
		initComponents();
	}

	private void initComponents() {

		panelSyncedFolders = new javax.swing.JPanel();
		scrollPaneSyncedFolders = new javax.swing.JScrollPane();
		listSyncedFolders = new javax.swing.JList<String>();
		panelSyncedFoldersButtons = new javax.swing.JPanel();
		buttonAddSyncedFolder = new javax.swing.JButton();
		buttonRemoveSyncedFolder = new javax.swing.JButton();
		panelSyncButtons = new javax.swing.JPanel();
		buttonStartUpload = new javax.swing.JButton();
		buttonStartDownload = new javax.swing.JButton();
		scrollPaneSyncStatus = new javax.swing.JScrollPane();
		textPaneSyncStatus = new javax.swing.JTextPane();
		menuBarMain = new javax.swing.JMenuBar();
		menuManage = new javax.swing.JMenu();
		menuItemManageKeys = new javax.swing.JMenuItem();
		menuItemManageConfig = new javax.swing.JMenuItem();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("CryptedCloud");
		setLocation(new java.awt.Point(20, 20));
		setPreferredSize(new java.awt.Dimension(700, 400));
		getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.PAGE_AXIS));

		panelSyncedFolders.setLayout(new javax.swing.BoxLayout(panelSyncedFolders, javax.swing.BoxLayout.PAGE_AXIS));

		listSyncedFolders.setModel(syncFolderListModel);
		listSyncedFolders.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPaneSyncedFolders.setViewportView(listSyncedFolders);

		panelSyncedFolders.add(scrollPaneSyncedFolders);

		panelSyncedFoldersButtons
				.setLayout(new javax.swing.BoxLayout(panelSyncedFoldersButtons, javax.swing.BoxLayout.LINE_AXIS));

		buttonAddSyncedFolder.setText("Add Folder");
		buttonAddSyncedFolder.setMaximumSize(new java.awt.Dimension(120, 30));
		buttonAddSyncedFolder.setMinimumSize(new java.awt.Dimension(120, 30));
		buttonAddSyncedFolder.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonAddSyncedFolderActionPerformed(evt);
			}
		});
		panelSyncedFoldersButtons.add(buttonAddSyncedFolder);

		buttonRemoveSyncedFolder.setText("Remove Folder");
		buttonRemoveSyncedFolder.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
		buttonRemoveSyncedFolder.setMaximumSize(new java.awt.Dimension(120, 30));
		buttonRemoveSyncedFolder.setMinimumSize(new java.awt.Dimension(120, 30));
		buttonRemoveSyncedFolder.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonRemoveSyncedFolderActionPerformed(evt);
			}
		});
		panelSyncedFoldersButtons.add(buttonRemoveSyncedFolder);

		panelSyncedFolders.add(panelSyncedFoldersButtons);

		getContentPane().add(panelSyncedFolders);

		panelSyncButtons.setLayout(new javax.swing.BoxLayout(panelSyncButtons, javax.swing.BoxLayout.LINE_AXIS));

		buttonStartUpload.setText("Start Upload");
		buttonStartUpload.setMaximumSize(new java.awt.Dimension(120, 30));
		buttonStartUpload.setMinimumSize(new java.awt.Dimension(120, 30));
		buttonStartUpload.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonStartUploadActionPerformed(evt);
			}
		});
		panelSyncButtons.add(buttonStartUpload);

		buttonStartDownload.setText("Start Download");
		buttonStartDownload.setMaximumSize(new java.awt.Dimension(120, 30));
		buttonStartDownload.setMinimumSize(new java.awt.Dimension(120, 30));
		buttonStartDownload.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonStartDownloadActionPerformed(evt);
			}
		});
		panelSyncButtons.add(buttonStartDownload);

		getContentPane().add(panelSyncButtons);

		scrollPaneSyncStatus.setPreferredSize(new java.awt.Dimension(33, 80));

		textPaneSyncStatus.setEditable(false);
		textPaneSyncStatus.setText("");
		textPaneSyncStatus.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
		textPaneSyncStatus.setMinimumSize(new java.awt.Dimension(28, 80));
		scrollPaneSyncStatus.setViewportView(textPaneSyncStatus);

		getContentPane().add(scrollPaneSyncStatus);

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

	private void buttonRemoveSyncedFolderActionPerformed(ActionEvent evt) {
		int selectedElement = listSyncedFolders.getSelectedIndex();
		if (selectedElement > -1 && selectedElement < syncFolderListModel.size()) {
			List<String> syncedFolders = new LinkedList<String>(Config.getInstance().getSyncedFolders());
			syncedFolders.remove(syncFolderListModel.get(selectedElement));
			writeSyncedFoldersToConfig(syncedFolders);
			recreateSyncedFoldersList();
		}
	}

	private void buttonAddSyncedFolderActionPerformed(ActionEvent evt) {
		final JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = fc.getSelectedFile();

		List<String> syncedFolders = new LinkedList<String>(Config.getInstance().getSyncedFolders());
		syncedFolders.add(file.getAbsolutePath());
		writeSyncedFoldersToConfig(syncedFolders);
		recreateSyncedFoldersList();
	}

	private void writeSyncedFoldersToConfig(final @Nonnull List<String> newSyncedFolders) {
		Config.WritableConfig newConfig = Config.getInstance().getWritableConfig();
		newConfig.getSyncedFolders().clear();
		newConfig.getSyncedFolders().addAll(newSyncedFolders);
		try {
			Config.writeAndReloadConfig(newConfig);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Cant write new config to disk: ", e);
			JOptionPane.showMessageDialog(this, "Can't write new config to disk: " + e.getMessage(), "ERROR",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void recreateSyncedFoldersList() {
		syncFolderListModel.clear();
		for (String syncFolder : Config.getInstance().getSyncedFolders()) {
			syncFolderListModel.addElement(syncFolder);
		}
	}

	private void buttonStartUploadActionPerformed(ActionEvent evt) {
		new SyncGUI(syncFolderListModel, textPaneSyncStatus).doSync(true);
	}

	private void buttonStartDownloadActionPerformed(ActionEvent evt) {
		new SyncGUI(syncFolderListModel, textPaneSyncStatus).doSync(false);
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
