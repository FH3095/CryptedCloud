package eu._4fh.cryptedcloud.files.encrypted;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.eclipse.jdt.annotation.NonNull;

import eu._4fh.cryptedcloud.crypt.FileDecrypter;
import eu._4fh.cryptedcloud.crypt.FileEncrypter;
import eu._4fh.cryptedcloud.files.CloudFile;

public class EncryptedFile implements CloudFile {
	private final @NonNull CloudFile file;
	private final @NonNull EncryptedService service;

	EncryptedFile(final @NonNull EncryptedService service, final @NonNull CloudFile file) throws IOException {
		this.service = service;
		this.file = file;
	}

	@Override
	public @NonNull String getFileName() {
		return service.getNameEncrypter().decryptName(file.getFileName());
	}

	@NonNull
	CloudFile getCloudFile() {
		return file;
	}

	@Override
	public @NonNull OutputStream getOutputStream() throws IOException {
		OutputStream out = new FileEncrypter(file.getOutputStream(), service.getPublicKeys());
		if (service.getDoCompress()) {
			out = new GZIPOutputStream(out);
		}
		return out;
	}

	@Override
	public @NonNull InputStream getInputStream() throws IOException {
		InputStream in = new FileDecrypter(file.getInputStream(), service.getPrivateKeys());
		if (service.getDoCompress()) {
			in = new GZIPInputStream(in);
		}
		return in;
	}
}
