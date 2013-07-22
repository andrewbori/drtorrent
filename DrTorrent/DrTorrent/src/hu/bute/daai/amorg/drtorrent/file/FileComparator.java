package hu.bute.daai.amorg.drtorrent.file;

import java.io.File;
import java.util.Comparator;

/** Sorting files in alphabetical order (directories first). */
public class FileComparator implements Comparator<File> {
	@Override
	public int compare(File lhs, File rhs) {
		if (lhs.isDirectory() && !rhs.isDirectory()) {
			return -1;
		} else if (!lhs.isDirectory() && rhs.isDirectory()) {
			return 1;
		}
		return lhs.compareTo(rhs);
	}
}
