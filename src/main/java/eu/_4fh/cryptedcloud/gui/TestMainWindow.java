package eu._4fh.cryptedcloud.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.abstractj.kalium.NaCl;
import org.abstractj.kalium.encoders.Encoder;
import org.abstractj.kalium.keys.KeyPair;

import eu._4fh.cryptedcloud.cloud.CloudService;
import eu._4fh.cryptedcloud.cloud.encrypted.EncryptedCloud;
import eu._4fh.cryptedcloud.cloud.google.GoogleCloud;
import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.crypt.FileDecrypter;
import eu._4fh.cryptedcloud.crypt.FileEncrypter;
import eu._4fh.cryptedcloud.sync.SyncUploader;
import eu._4fh.cryptedcloud.util.LogConfigRefresh;
import eu._4fh.cryptedcloud.util.Util;

public class TestMainWindow extends JFrame {
	private static final Logger log = Util.getLogger();
	private static final long serialVersionUID = -6336899806926732538L;

	public static void main(String[] args) {
		new LogConfigRefresh();
		try {
			Config.readConfig();
		} catch (IOException e) {
			log.log(Level.SEVERE, "Cant read config: ", e);
			System.exit(1);
		}
		NaCl.init();
		log.info("System initialized");
		// new TestMainWindow();
		try {
			System.out.println("Hello World!");
			// eu._4fh.Main.main(args);

			KeyPair keyPair = getKeyPair();
			FileEncrypter encrypter = new FileEncrypter();
			encrypter.addKey(keyPair.getPublicKey());
			FileDecrypter decrypter = new FileDecrypter(keyPair);
			CloudService cloud = new EncryptedCloud(new GoogleCloud(), encrypter, decrypter);
			SyncUploader up = new SyncUploader(System.out,
					Collections.singletonList(new File("C:/Users/FH/Desktop/CryptedCloud/SyncFolder")),
					cloud.getRootFolder(), cloud);
			if (!up.doSync()) {
				log.warning("Sync unsuccessfull. Check log!");
			}
			System.out.println(cloud.getRootFolder().toStringRecursive());
			cloud.getRootFolder().getSubFolders().get("C:\\Users\\FH\\Desktop\\CryptedCloud\\SyncFolder").getFiles()
					.get("livestreamer.cmd").downloadFile(new File("C:/Users/FH/Desktop/CryptedCloud/Down.txt"));
			// new SftpCloud().finishSync(true);
			System.out.println("Press [Enter] to end");
			// System.in.read();
		} catch (IOException | GeneralSecurityException e) {
			log.log(Level.SEVERE, "Error while Syncing", e);
		}
	}

	static private KeyPair getKeyPair() {
		File keyFile = new File(Config.getInstance().getConfigDir(), "FileKey.keydata");
		if (keyFile.exists() && keyFile.isFile()) {
			try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(keyFile)))) {
				return new KeyPair(Encoder.HEX.decode(in.readUTF()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		KeyPair keyPair = new KeyPair();
		try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(keyFile)))) {
			out.writeUTF(Encoder.HEX.encode(keyPair.getPrivateKey().toBytes()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return keyPair;
	}

	public TestMainWindow() {
		super("Der Quadrator");
		// log=Util.getLogger(getClass());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new FlowLayout());

		JTextField textField = new JTextField("Zahl eingeben");
		getContentPane().add(textField);

		JButton button = new JButton("Quadrieren");
		getContentPane().add(button);

		JLabel label = new JLabel("Ergebnis");
		getContentPane().add(label);

		button.addActionListener(new MyListener(textField, label));

		createMenuBar();

		setLocation(250, 190);
		setSize(300, 90);
		setVisible(true);
	}

	private void createMenuBar() {
		// Where the GUI is created:
		JMenuBar menuBar;
		JMenu menu, submenu;
		JMenuItem menuItem;
		JRadioButtonMenuItem rbMenuItem;
		JCheckBoxMenuItem cbMenuItem;

		// Create the menu bar.
		menuBar = new JMenuBar();

		// Build the first menu.
		menu = new JMenu("A Menu");
		menu.setMnemonic(KeyEvent.VK_A);
		menu.getAccessibleContext().setAccessibleDescription("The only menu in this program that has menu items");
		menuBar.add(menu);

		// a group of JMenuItems
		menuItem = new JMenuItem("A text-only menu item", KeyEvent.VK_T);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
		menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
		menu.add(menuItem);

		menuItem = new JMenuItem("Both text and icon", new ImageIcon("images/middle.gif"));
		menuItem.setMnemonic(KeyEvent.VK_B);
		menu.add(menuItem);

		menuItem = new JMenuItem(new ImageIcon("images/middle.gif"));
		menuItem.setMnemonic(KeyEvent.VK_D);
		menu.add(menuItem);

		// a group of radio button menu items
		menu.addSeparator();
		ButtonGroup group = new ButtonGroup();
		rbMenuItem = new JRadioButtonMenuItem("A radio button menu item");
		rbMenuItem.setSelected(true);
		rbMenuItem.setMnemonic(KeyEvent.VK_R);
		group.add(rbMenuItem);
		menu.add(rbMenuItem);

		rbMenuItem = new JRadioButtonMenuItem("Another one");
		rbMenuItem.setMnemonic(KeyEvent.VK_O);
		group.add(rbMenuItem);
		menu.add(rbMenuItem);

		// a group of check box menu items
		menu.addSeparator();
		cbMenuItem = new JCheckBoxMenuItem("A check box menu item");
		cbMenuItem.setMnemonic(KeyEvent.VK_C);
		menu.add(cbMenuItem);

		cbMenuItem = new JCheckBoxMenuItem("Another one");
		cbMenuItem.setMnemonic(KeyEvent.VK_H);
		menu.add(cbMenuItem);

		// a submenu
		menu.addSeparator();
		submenu = new JMenu("A submenu");
		submenu.setMnemonic(KeyEvent.VK_S);

		menuItem = new JMenuItem("An item in the submenu");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.ALT_MASK));
		submenu.add(menuItem);

		menuItem = new JMenuItem("Another item");
		submenu.add(menuItem);
		menu.add(submenu);

		// Build second menu in the menu bar.
		menu = new JMenu("Another Menu");
		menu.setMnemonic(KeyEvent.VK_N);
		menu.getAccessibleContext().setAccessibleDescription("This menu does nothing");
		menuBar.add(menu);

		setJMenuBar(menuBar);
	}

	public class MyListener implements ActionListener {
		private JTextField textField;
		private JLabel label;

		public MyListener(JTextField tf, JLabel l) {
			textField = tf;
			label = l;
		}

		public void actionPerformed(ActionEvent ae) {
			String text = textField.getText();

			String ergebnisText = "";
			try {
				double zahl = Double.parseDouble(text);
				double quadrat = zahl * zahl;
				ergebnisText = "" + quadrat;
			} catch (NumberFormatException ex) {
				ergebnisText = "keine Zahl";
			}

			label.setText(ergebnisText);
			textField.setText("Zahl eingeben");
			log.finer("Ergebnis=" + ergebnisText);
		}
	}
}
