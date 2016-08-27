package eu._4fh.cryptedcloud.files.raw;

import java.io.File;

import org.eclipse.jdt.annotation.NonNull;

import eu._4fh.cryptedcloud.config.Config;
import eu._4fh.cryptedcloud.files.AbstractCloudService;
import eu._4fh.cryptedcloud.files.CloudFolder;
import eu._4fh.cryptedcloud.files.CloudService;
import eu._4fh.cryptedcloud.util.Util;

public class RawService extends AbstractCloudService implements CloudService {
	private final @NonNull RawFolder rootFolder;

	public RawService(final @NonNull File targetDir) {
		this.rootFolder = new RawFolder(this, targetDir);
	}

	@Override
	public @NonNull CloudFolder getRootFolder() {
		return rootFolder;
	}

	@Override
	public @NonNull String getUserName() {
		return "LocalUser";
	}

	@Override
	public Util.Pair<Long, Long> getUsageAndLimit() {
		File targetFolder = Config.getInstance().getTargetDir();
		return new Util.Pair<Long, Long>(targetFolder.getTotalSpace() - targetFolder.getUsableSpace(),
				targetFolder.getTotalSpace());
	}

	@Override
	public @NonNull Long getFreeSpace() {
		return rootFolder.getFolder().getUsableSpace();
	}
}
