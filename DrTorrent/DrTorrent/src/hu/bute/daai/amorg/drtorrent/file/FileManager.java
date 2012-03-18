package hu.bute.daai.amorg.drtorrent.file;

import hu.bute.daai.amorg.drtorrent.torrentengine.Torrent;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

	/** Returns whether or not the file already exists. */
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
		} catch (Exception e) {
			e.printStackTrace();
			Log.v("FileManager", e.getMessage());
			return false;
		} finally {
			if (file != null) {
				try {
					file.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return true;
	}
	
	/** Returns the content of the file in a byte array. */
	public static byte[] readFile(String filePath) {
		BufferedInputStream bis = null;
		try {
			File file = new File(filePath);
			bis = new BufferedInputStream(new FileInputStream(file));
			byte[] bf = new byte[(int) file.length()];
			bis.read(bf);
			return bf;
		} catch (Exception ex) {
			return null;
		} finally {
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/** Writes a block to the file in the given position. */
	public int writeFile(String filePath, int filePosition, byte[] block) {
		return writeFile(filePath, filePosition, block, 0, block.length);
	}

	/** 
	 * Writes a block to the file in the given position.
	 * The offset and length within the block is also given. 
	 */
	public synchronized int writeFile(String filePath, int filePosition, byte[] block, int offset, int length) {
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(filePath, "rw");
			file.seek(filePosition);
			file.write(block, offset, length);
			return Torrent.ERROR_NONE;
		} catch (IOException e) {
			// Log.v(LOG_TAG, "[File] " + e.getMessage());
			return Torrent.ERROR_GENERAL;
		} finally {
			try {
				file.close();
			} catch (Exception e) {}
		}
	}

}
