package eu._4fh.cryptedcloud.cloud;

import javax.annotation.Nonnull;

public abstract class AbstractCloudFolder implements CloudFolder {
	public @Nonnull String toStringRecursive() {
		StringBuffer buff = new StringBuffer();
		toStringRecursive(0, buff);
		return buff.toString().trim();
	}

	private void toStringRecursive(int level, StringBuffer buff) {
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

	private void indentLine(int level, StringBuffer buff) {
		buff.append('\n');
		for (int i = 0; i < level * 2; ++i) {
			buff.append(' ');
		}
		buff.append('-').append(' ');
	}
}
