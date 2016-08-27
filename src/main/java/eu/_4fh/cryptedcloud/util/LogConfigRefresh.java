package eu._4fh.cryptedcloud.util;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

public class LogConfigRefresh implements Runnable {
	private static final Logger log = Util.getLogger();
	private static final long LOG_REFRESH_INTERVAL = TimeUnit.SECONDS.toMillis(15);
	private final File logConfigFile;
	private boolean foundFilePreviously;

	public LogConfigRefresh() {
		this("logging.properties");
	}

	public LogConfigRefresh(final @NonNull String configFile) {
		logConfigFile = new File(configFile);
		System.setProperty("java.util.logging.config.file", logConfigFile.getAbsolutePath());
		// In the beginning we want a log-message in every case
		foundFilePreviously = !logConfigFile.exists();
		refreshLogConfig();
		Thread thread = new Thread(this, "LogConfigRefresh");
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	public void run() {
		while (true) {
			try {
				try {
					Thread.sleep(LOG_REFRESH_INTERVAL);
				} catch (InterruptedException e) {
					// Ignore
				}
				refreshLogConfig();
			} catch (Throwable t) {
				if (log.isLoggable(Level.SEVERE)) {
					log.log(Level.SEVERE, "Error in LogConfigRefresh", t);
				} else {
					t.printStackTrace();
				}
			}
		}
	}

	private void refreshLogConfig() {
		if (logConfigFile.exists()) {
			// Log manager must read configuration before first log-line is
			// printed - otherwise, log-config is ignored!
			try {
				LogManager.getLogManager().readConfiguration();
			} catch (SecurityException | IOException e) {
				if (log.isLoggable(Level.SEVERE)) {
					log.log(Level.SEVERE, "Cant reload log config", e);
				} else {
					e.printStackTrace();
				}
			}
			if (!foundFilePreviously) {
				String msg = "Found new Log-Config: " + logConfigFile.getAbsolutePath();
				if (log.isLoggable(Level.INFO)) {
					log.log(Level.INFO, msg);
				} else {
					System.out.println(msg);
				}
				foundFilePreviously = true;
			}
		} else if (foundFilePreviously) {
			String msg = "Missing Log-Config: " + logConfigFile.getAbsolutePath();
			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, msg);
			} else {
				System.out.println(msg);
			}
			foundFilePreviously = false;
		}
	}
}
