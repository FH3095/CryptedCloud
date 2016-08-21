package eu._4fh.cryptedcloud.cloud.google;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import eu._4fh.cryptedcloud.cloud.CloudFile;
import eu._4fh.cryptedcloud.util.Util;

public class GoogleFile implements CloudFile {
	private static final Logger log = Util.getLogger();
	private File file;
	private final GoogleCloud cloud;

	GoogleFile(@Nonnull GoogleCloud cloud, @Nonnull File file) {
		this.file = file;
		this.cloud = cloud;
	}

	@Override
	public @Nonnull String getFileName() {
		return file.getName();
	}

	File getFile() {
		return file;
	}

	@Override
	public void updateFile(java.io.File file) throws IOException {
		try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
			InputStreamContent stream = new InputStreamContent("application/octet-stream", inputStream);
			stream.setLength(file.length());
			this.file = cloud.getDrive().files().update(this.file.getId(), null, stream)
					.setFields(GoogleCloud.FILE_FIELDS).execute();
		}
	}

	@Override
	public void downloadFile(java.io.File file) throws IOException {
		try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(file))) {
			cloud.getDrive().files().get(this.file.getId()).executeMediaAndDownloadTo(stream);
		}
		log.finer(() -> "Downloaded file " + this.file.getName() + " (" + this.file.getId() + ") to "
				+ file.getAbsolutePath());
	}

	@Override
	public void downloadFile(final @Nonnull java.io.File file, final int firstByte, final int lastByte)
			throws IOException {
		if (firstByte < 0 || (lastByte != CloudFile.LAST_BYTES_UNDEF_VALUE && lastByte <= firstByte)) {
			throw new IllegalArgumentException(
					"FirstByte must be >= 0 and lastByte must be > firstByte or " + CloudFile.LAST_BYTES_UNDEF_VALUE
							+ ". But the values are: firstByte=" + firstByte + " ; lastByte=" + lastByte);
		}
		if (firstByte == 0 && lastByte == CloudFile.LAST_BYTES_UNDEF_VALUE) {
			downloadFile(file);
			return;
		}
		try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(file))) {
			Drive.Files.Get getCommand = cloud.getDrive().files().get(this.file.getId());
			// Looks like a download with a fixed length is only allowed when
			// directDownload is enabled (could be an error in the google java
			// library)
			getCommand.getMediaHttpDownloader().setDirectDownloadEnabled(true);
			if (lastByte == CloudFile.LAST_BYTES_UNDEF_VALUE) {
				getCommand.getMediaHttpDownloader().setBytesDownloaded(firstByte);
			} else {
				getCommand.getMediaHttpDownloader().setContentRange(firstByte, lastByte);
			}
			getCommand.executeMediaAndDownloadTo(stream);
		}
		log.finer(() -> "Downloaded file " + this.file.getName() + " (" + this.file.getId() + ") from byte " + firstByte
				+ " to " + lastByte + " to " + file.getAbsolutePath());
	}

	public String toString() {
		return "[" + file.getName() + ";" + file.toString() + "]";
	}

	@Override
	public long getLastModification() throws IOException {
		long millis = file.getModifiedTime().getValue();
		return TimeUnit.MILLISECONDS.toSeconds(millis);
	}
}
