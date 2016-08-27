package eu._4fh.cryptedcloud.files;

import org.eclipse.jdt.annotation.NonNull;

public abstract class AbstractCloudFolder implements CloudFolder {
	@SuppressWarnings("null")
	public @NonNull String toStringRecursive() {
		StringBuffer buff = new StringBuffer();
		toStringRecursive(0, buff);
		return buff.toString().trim();
	}

	private void toStringRecursive(int level, final @NonNull StringBuffer buff) {
		indentLine(level, buff);
		buff.append(getFolderName());
		level++;
		for (CloudFile file : getFiles().values()) {
			indentLine(level, buff);
			buff.append(file.getFileName());
		}
		for (CloudFolder folder : getSubFolders().values()) {
			((AbstractCloudFolder) folder).toStringRecursive(level, buff);
		}
	}

	private void indentLine(final int level, final @NonNull StringBuffer buff) {
		buff.append('\n');
		for (int i = 0; i < level * 2; ++i) {
			buff.append(' ');
		}
		buff.append('-').append(' ');
	}
}
