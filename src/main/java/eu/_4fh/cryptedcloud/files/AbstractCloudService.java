package eu._4fh.cryptedcloud.files;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

import eu._4fh.cryptedcloud.util.Util.Pair;

public abstract class AbstractCloudService implements CloudService {
	protected static interface TreeBuilderHelper<FolderType extends CloudFolder, FileType extends CloudFile> {
		boolean isFolder(final Object fileOrFolder);

		FolderType constructFolder(final Object folder, final FolderType parent);

		FileType constructFile(final Object file, final FolderType parent);

		String getFolderId(final FolderType folder);

		String getParentId(final Object fileOrFolder);
	}

	protected <FolderType extends CloudFolder> void constructTree(final @NonNull Map<String, ? extends Object> files,
			final @NonNull FolderType rootFolder,
			final @NonNull TreeBuilderHelper<FolderType, ? extends CloudFile> treeBuilderHelper) {
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
	public @NonNull Long getFreeSpace() throws IOException {
		Pair<Long, Long> usageAndLimit = getUsageAndLimit();
		return usageAndLimit.value2 - usageAndLimit.value1;
	}
}
