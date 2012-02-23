package hu.bute.daai.amorg.drtorrent.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;

import android.os.StatFs;
import android.util.Log;

/** Class managing the file system. */
public class FileManager {

	/** Returns the size of free space in bytes. */
	public static long freeSize(String path) {
		try {
			StatFs fs = new StatFs("/sdcard/"); // path);
			return (long) fs.getAvailableBlocks() * fs.getBlockSize();
		} catch (Exception e) {
			return 5 * 1024L * 1024L * 1024L;
		}
	}

	/** Returns whether the file already exists */
	public static boolean fileExists(String filepath) {
		return (new File(filepath)).exists();
	}

	/** Creates a new file and reserves free space for it. */
	public static boolean createFile(String filePath, int fileSize) {
		RandomAccessFile file = null;

		try {
			String dirPath = filePath.substring(0, filePath.lastIndexOf('/'));
			File dir = new File(dirPath);
			dir.mkdirs();

			File f = new File(filePath);
			f.createNewFile();

			file = new RandomAccessFile(f, "rw");
			file.setLength(fileSize);
			file.close();
		} catch (Exception e) {
			e.printStackTrace();
			Log.v("FileManager", e.getMessage());
			return false;
		}

		return true;
	}

	/** Returns the content of the file in a byte array. */
	public static byte[] readFile(String filepath) {
		try {
			File file = new File(filepath);
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
			byte[] bf = new byte[(int) file.length()];
			in.read(bf);
			in.close();
			return bf;
		} catch (Exception ex) {
			return null;
		}
	}
}
