package eu._4fh.cryptedcloud.cloud.sftp;

// Disabled for now. Also removed sshj4 and slf4j-jdk14 from pom.xml

/*import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PublicKey;
import java.util.Collections;

import javax.annotation.Nonnull;

import eu._4fh.cryptedcloud.cloud.AbstractCloudService;
import eu._4fh.cryptedcloud.cloud.CloudFolder;
import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.util.Util.Pair;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;

public class SftpCloud extends AbstractCloudService {
	private final @Nonnull SSHClient ssh;
	private final @Nonnull SFTPClient sftp;

	public SftpCloud() throws IOException {
		// TODO Auto-generated constructor stub
		ssh = new SSHClient();
		ssh.getConnection().setMaxPacketSize(10 * 1024 * 1024);
		ssh.getConnection().setWindowSize(10 * 1024 * 1024);
		ssh.loadKnownHosts();
		ssh.addHostKeyVerifier(new HostKeyVerifier() {
			@Override
			public boolean verify(String hostname, int port, PublicKey key) {
				return true;
			}
		});
		ssh.connect(Config.getInstance().getSftpConfig().getHostName(),
				Config.getInstance().getSftpConfig().getHostPort());
		ssh.getConnection().setMaxPacketSize(10 * 1024 * 1024);
		ssh.getConnection().setWindowSize(10 * 1024 * 1024);
		ssh.authPublickey(Config.getInstance().getSftpConfig().getUserName(),
				Config.getInstance().getSftpConfig().getDataStoreDir().getAbsolutePath());
		sftp = ssh.newSFTPClient();
		try (RemoteFile file = sftp.open("/home/fh_unsafe/programs/cryptedcloud/test.txt",
				Collections.singleton(OpenMode.READ))) {
			try (InputStream in = file.new ReadAheadRemoteFileInputStream(3);
					OutputStream out = new BufferedOutputStream(
							new FileOutputStream("C:/Users/FH/Desktop/CryptedCloud/SftpTest.txt"));) {
				byte[] buffer = new byte[1024 * 1024];
				int readBytes = 0;
				while ((readBytes = in.read(buffer)) > -1) {
					out.write(buffer, 0, readBytes);
				}
			}
		}
		try (RemoteFile file = sftp.open("/home/fh_unsafe/programs/cryptedcloud/test_large.txt",
				Collections.singleton(OpenMode.READ))) {
			try (InputStream in = file.new ReadAheadRemoteFileInputStream(3);
					OutputStream out = new BufferedOutputStream(
							new FileOutputStream("C:/Users/FH/Desktop/CryptedCloud/SftpTestLarge.txt"));) {
				byte[] buffer = new byte[10 * 1024 * 1024];
				int readBytes = 0;
				while ((readBytes = in.read(buffer)) > -1) {
					out.write(buffer, 0, readBytes);
				}
			}
		}
	}

	public void finalize() {
		try {
			sftp.close();
		} catch (IOException e) {
			// Ignore
		}
		try {
			ssh.close();
		} catch (IOException e) {
			// Ignore
		}
	}

	@Override
	public CloudFolder getRootFolder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUserName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Pair<Long, Long> getUsageAndLimit(boolean refresh) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void finishSync(final boolean upload, final boolean wasSuccessfull) {
		try {
			sftp.close();
		} catch (IOException e) {
			// Ignore
		}
		try {
			ssh.close();
		} catch (IOException e) {
			// Ignore
		}
	}
}*/
