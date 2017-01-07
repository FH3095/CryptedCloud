package eu._4fh.cryptedcloud.gui;

import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.util.Util;

public class ConfigWindow extends javax.swing.JFrame {

	private static final long serialVersionUID = 7270204030452321058L;
	private static final Logger log = Util.getLogger();

	public ConfigWindow() {
		initComponents();
	}

	private void initComponents() {

		labelFileChunkSize = new javax.swing.JLabel();
		formattedFileChunkSize = new javax.swing.JFormattedTextField();
		labelConfigDir = new javax.swing.JLabel();
		stringConfigDir = new javax.swing.JTextField();
		labelTempDir = new javax.swing.JLabel();
		stringTempDir = new javax.swing.JTextField();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("Config");
		addWindowListener(new OnCloseWriteConfigWindowListener());
		getContentPane().setLayout(new java.awt.GridLayout(6, 2));

		labelFileChunkSize.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		labelFileChunkSize.setLabelFor(formattedFileChunkSize);
		labelFileChunkSize.setText("File Chunk Size");
		labelFileChunkSize.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 5));
		getContentPane().add(labelFileChunkSize);

		NumberFormatter formattedFileChunkSizeFormatter = new NumberFormatter(NumberFormat.getIntegerInstance());
		formattedFileChunkSizeFormatter.setMinimum(new Integer(1));
		formattedFileChunkSizeFormatter.setMaximum(new Integer(Integer.MAX_VALUE - 1));
		formattedFileChunkSizeFormatter.setAllowsInvalid(false);
		formattedFileChunkSize.setFormatterFactory(new DefaultFormatterFactory(formattedFileChunkSizeFormatter));
		getContentPane().add(formattedFileChunkSize);

		labelConfigDir.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		labelConfigDir.setLabelFor(stringConfigDir);
		labelConfigDir.setText("Config Dir");
		labelConfigDir.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 5));
		getContentPane().add(labelConfigDir);

		stringConfigDir.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				stringFieldDirectoryChooser(evt);
			}
		});
		getContentPane().add(stringConfigDir);

		labelTempDir.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		labelTempDir.setLabelFor(stringTempDir);
		labelTempDir.setText("Temp Dir");
		labelTempDir.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 5));
		getContentPane().add(labelTempDir);

		stringTempDir.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				stringFieldDirectoryChooser(evt);
			}
		});
		getContentPane().add(stringTempDir);

		// Init Values from Config
		Config config = Config.getInstance();
		formattedFileChunkSize.setText(Integer.toString(config.getFileChunkSize()));
		stringConfigDir.setText(config.getConfigDir().getAbsolutePath());
		stringTempDir.setText(config.getTempDir().getAbsolutePath());

		pack();
	}

	private void stringFieldDirectoryChooser(java.awt.event.MouseEvent evt) {
		if (evt.getButton() != MouseEvent.BUTTON1 || !(evt.getComponent() instanceof JTextField)
				|| evt.getClickCount() != 1) {
			return;
		}
		final JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = fc.getSelectedFile();
		((JTextField) evt.getComponent()).setText(file.getAbsolutePath());
		evt.consume();
	}

	private class OnCloseWriteConfigWindowListener implements WindowListener {
		@Override
		public void windowClosing(WindowEvent evt) {
			synchronized (OnCloseWriteConfigWindowListener.class) {
				try {
					Config.WritableConfig config = Config.getInstance().getWritableConfig();
					config.setFileChunkSize(
							NumberFormat.getIntegerInstance().parse(formattedFileChunkSize.getText()).intValue());
					config.setConfigDir(new File(stringConfigDir.getText()));
					config.setTempDir(new File(stringTempDir.getText()));
					Config.writeAndReloadConfig(config);
				} catch (IOException | ParseException e) {
					log.log(Level.SEVERE, "Cant write config: ", e);
					JOptionPane.showMessageDialog(ConfigWindow.this, "Cant write config: " + e.getMessage(), "ERROR",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}

		@Override
		public void windowOpened(WindowEvent e) {
		}

		@Override
		public void windowClosed(WindowEvent e) {
		}

		@Override
		public void windowIconified(WindowEvent e) {
		}

		@Override
		public void windowDeiconified(WindowEvent e) {
		}

		@Override
		public void windowActivated(WindowEvent e) {
		}

		@Override
		public void windowDeactivated(WindowEvent e) {
		}
	}

	private javax.swing.JFormattedTextField formattedFileChunkSize;
	private javax.swing.JLabel labelConfigDir;
	private javax.swing.JLabel labelFileChunkSize;
	private javax.swing.JLabel labelTempDir;
	private javax.swing.JTextField stringConfigDir;
	private javax.swing.JTextField stringTempDir;
}
