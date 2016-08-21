package eu._4fh.cryptedcloud.config;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.annotation.Nonnull;

public class SftpConfig implements SubConfig {
	static final String CONFIG_FILE_NAME = "sftp";
	private String hostName;
	private int hostPort;
	private String userName;
	private String cryptedCloudRootFolderName;

	public SftpConfig() {
		hostName = "ssh.4fh.eu";
		hostPort = 22;
		userName = "fh_unsafe";
		cryptedCloudRootFolderName = "CrytpedCloud";
	}

	public SftpConfig(final @Nonnull String hostName, final int hostPort, final @Nonnull String userName,
			final @Nonnull String cryptedCloudRootFolderName) {
		this.hostName = hostName;
		this.hostPort = hostPort;
		this.userName = userName;
		this.cryptedCloudRootFolderName = cryptedCloudRootFolderName;
	}

	public void writeToFile(final @Nonnull DataOutputStream out) throws IOException {
		out.writeUTF(CONFIG_FILE_NAME);
		out.writeInt(1);
		out.writeUTF(hostName);
		out.writeInt(hostPort);
		out.writeUTF(userName);
		out.writeUTF(cryptedCloudRootFolderName);
	}

	public void readFromFile(final int version, final @Nonnull DataInputStream in) throws IOException {
		hostName = in.readUTF();
		hostPort = in.readInt();
		userName = in.readUTF();
		cryptedCloudRootFolderName = in.readUTF();
	}

	public @Nonnull String getCrytpedCloudRootFolderName() {
		return cryptedCloudRootFolderName;
	}

	public @Nonnull String getHostName() {
		return hostName;
	}

	public int getHostPort() {
		return hostPort;
	}

	public @Nonnull String getUserName() {
		return userName;
	}

	@Override
	public SftpConfig clone() {
		return new SftpConfig(this.hostName, this.hostPort, this.userName, this.cryptedCloudRootFolderName);
	}
}
