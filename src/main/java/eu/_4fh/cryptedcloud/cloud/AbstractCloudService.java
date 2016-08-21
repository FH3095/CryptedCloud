package eu._4fh.cryptedcloud.cloud;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nonnull;

import eu._4fh.cryptedcloud.util.Util.Pair;

public abstract class AbstractCloudService implements CloudService {
	protected static interface TreeBuilderHelper<FolderType extends CloudFolder, FileType extends CloudFile> {
		boolean isFolder(final Object fileOrFolder);

		FolderType constructFolder(final Object folder, final FolderType parent);

		FileType constructFile(final Object file, final FolderType parent);

		String getFolderId(final FolderType folder);

		String getParentId(final Object fileOrFolder);
	}

	protected <FolderType extends CloudFolder> void constructTree(final @Nonnull Map<String, ? extends Object> files,
			final @Nonnull FolderType rootFolder,
			final @Nonnull TreeBuilderHelper<FolderType, ? extends CloudFile> treeBuilderHelper) {
		Map<String, FolderType> newTreeFolders = new HashMap<String, FolderType>();
		newTreeFolders.put(treeBuilderHelper.getFolderId(rootFolder), rootFolder);

		boolean changed = true;
		while (changed) {
			changed = false;
			Map<String, FolderType> nextNewTreeFolders = new HashMap<String, FolderType>();
			Iterator<? extends Object> fileIt = files.values().iterator();
			while (fileIt.hasNext()) {
				Object fileOrFolder = fileIt.next();
				if (newTreeFolders.containsKey(treeBuilderHelper.getParentId(fileOrFolder))) {
					if (treeBuilderHelper.isFolder(fileOrFolder)) {
						FolderType newFolder = treeBuilderHelper.constructFolder(fileOrFolder,
								newTreeFolders.get(treeBuilderHelper.getParentId(fileOrFolder)));
						nextNewTreeFolders.put(treeBuilderHelper.getFolderId(newFolder), newFolder);
						changed = true;
					} else {
						treeBuilderHelper.constructFile(fileOrFolder,
								newTreeFolders.get(treeBuilderHelper.getParentId(fileOrFolder)));
					}
					fileIt.remove();
				}
			}
			newTreeFolders = nextNewTreeFolders;
		}
	}

	@Override
	public @Nonnull Long getFreeSpace(boolean refresh) throws IOException {
		Pair<Long, Long> usageAndLimit = getUsageAndLimit(refresh);
		return usageAndLimit.value2 - usageAndLimit.value1;
	}
}
