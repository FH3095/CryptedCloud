package eu._4fh.cryptedcloud;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.abstractj.kalium.NaCl;
import org.apache.commons.cli.ParseException;

import eu._4fh.cryptedcloud.cmdline.MainCmd;
import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.gui.MainWindow;
import eu._4fh.cryptedcloud.util.LogConfigRefresh;
import eu._4fh.cryptedcloud.util.Util;

public class Main {
	private static final Logger log = Util.getLogger();

	public static void main(String args[]) {
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

		if (args.length < 1 && !GraphicsEnvironment.isHeadless()) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException e) {
				System.out.println("Cant set LookAndFeel to SystemLookAndFeel: ");
				e.printStackTrace(System.out);
			}
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					new MainWindow().setVisible(true);
				}
			});
		} else {
			String jarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath())
					.getName();
			if (jarFile.isEmpty()) {
				jarFile = "JarFile.jar";
			}
			try {
				new MainCmd(jarFile, args).run();
			} catch (ParseException e) {
				System.out.println("Cant parse command line arguments:");
				e.printStackTrace(System.out);
			} catch (Throwable t) {
				log.log(Level.SEVERE, "Cant parse command line arguments", t);
			}
		}
	}
}
