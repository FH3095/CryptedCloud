package eu._4fh.cryptedcloud.files.raw;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.jdt.annotation.NonNull;

import eu._4fh.cryptedcloud.files.CloudFile;

public class RawFile implements CloudFile {
	private final @NonNull File file;

	RawFile(final @NonNull RawService service, final @NonNull File file) {
		if (file.isDirectory()) {
			throw new IllegalArgumentException(
					"Exptected \"" + file.getAbsolutePath() + "\" to be a file, not a directory!");
		}
		this.file = file;
	}

	@SuppressWarnings("null")
	@Override
	public @NonNull String getFileName() {
		return file.getName();
	}

	@Override
	public @NonNull OutputStream getOutputStream() throws IOException {
		return new BufferedOutputStream(new FileOutputStream(file));
	}

	@Override
	public @NonNull InputStream getInputStream() throws IOException {
		return new BufferedInputStream(new FileInputStream(file));
	}

	@NonNull
	File getFile() {
		return file;
	}
}
