package eu._4fh.cryptedcloud.config;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.annotation.Nonnull;

public class GoogleConfig implements SubConfig {
	static final String CONFIG_FILE_NAME = "google";
	private String userEMail;
	private String cryptedCloudRootFolderName;

	public GoogleConfig() {
		cryptedCloudRootFolderName = "CryptedCloud";
		userEMail = "";
	}

	public GoogleConfig(final @Nonnull String userEMail, final @Nonnull String cryptedCloudRootFolderName) {
		this.userEMail = userEMail;
		this.cryptedCloudRootFolderName = cryptedCloudRootFolderName;
	}

	public void writeToFile(final @Nonnull DataOutputStream out) throws IOException {
		out.writeUTF(CONFIG_FILE_NAME);
		out.writeInt(1);
		out.writeUTF(userEMail);
		out.writeUTF(cryptedCloudRootFolderName);
	}

	public void readFromFile(final int version, final @Nonnull DataInputStream in) throws IOException {
		userEMail = in.readUTF();
		cryptedCloudRootFolderName = in.readUTF();
	}

	public @Nonnull String getCrytpedCloudRootFolderName() {
		return cryptedCloudRootFolderName;
	}

	public @Nonnull String getUserEMail() {
		return userEMail;
	}

	@Override
	public GoogleConfig clone() {
		return new GoogleConfig(this.userEMail, this.cryptedCloudRootFolderName);
	}
}
