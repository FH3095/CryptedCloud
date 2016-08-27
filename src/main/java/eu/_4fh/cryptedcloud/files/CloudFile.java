package eu._4fh.cryptedcloud.files;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.jdt.annotation.NonNull;

public interface CloudFile {
	public @NonNull String getFileName();

	public @NonNull OutputStream getOutputStream() throws IOException;

	public @NonNull InputStream getInputStream() throws IOException;
}
