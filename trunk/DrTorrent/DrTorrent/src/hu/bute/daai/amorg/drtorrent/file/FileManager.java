package hu.bute.daai.amorg.drtorrent.file;

import hu.bute.daai.amorg.drtorrent.torrentengine.Torrent;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;

import android.os.StatFs;
import android.util.Log;

/** Class managing the file system. */
public class FileManager {
	private final static String LOG_TAG = "FileManager";
	
	final private Torrent torrent_;
	final private Vector<hu.bute.daai.amorg.drtorrent.torrentengine.File> filesCreated_;
	final private Vector<hu.bute.daai.amorg.drtorrent.torrentengine.File> filesToCreate_;

	private boolean isCreating_ = false;
	
	public FileManager(final Torrent torrent) {
		torrent_ = torrent;
		filesToCreate_ = new Vector<hu.bute.daai.amorg.drtorrent.torrentengine.File>();
		filesCreated_ = new Vector<hu.bute.daai.amorg.drtorrent.torrentengine.File>();
	}	
	
	/** Adds a new file to create. Returns true if it has not been added yet. */
	public boolean addFile(final hu.bute.daai.amorg.drtorrent.torrentengine.File file) {
		if (!filesCreated_.contains(file) && !filesToCreate_.contains(file)) {
			filesToCreate_.addElement(file);
			return true;
		}
		return false;
	}
	
	public boolean isCreating() {
		return isCreating_;
	}

	public void createFiles() {
		isCreating_ = true;
		
		for (int i = 0; i < filesToCreate_.size() && torrent_.isWorking(); i++) {
			hu.bute.daai.amorg.drtorrent.torrentengine.File fileInfo = filesToCreate_.elementAt(i);
			createFile(fileInfo);
			
			if (fileInfo.getSize() == fileInfo.getCreatedSize()) {
				filesCreated_.addElement(fileInfo);
				filesToCreate_.removeElementAt(i);
				i--;
			}
		}
		
		isCreating_ = false;
	}
	
	public void createFile(final hu.bute.daai.amorg.drtorrent.torrentengine.File fileInfo) {
		final String filePath = fileInfo.getFullPath();
		long fileSize = fileInfo.getSize();
		
		RandomAccessFile file = null;

		try {
			String dirPath = filePath.substring(0, filePath.lastIndexOf('/'));
			
			{
				final File dir = new File(dirPath);
				dir.mkdirs();
			}

			{
				final File f = new File(filePath);
				f.createNewFile();
			
				file = new RandomAccessFile(f, "rw");
			}
			
			if (file.length() != fileSize) {
				for (long size = file.length(); size < fileSize && torrent_.isWorking(); size += torrent_.getPieceLength()) {
					long newSize = size + torrent_.getPieceLength(); 
					if (newSize >= fileSize) {
						newSize = fileSize;
					}
					file.setLength(newSize);
					fileInfo.setCreatedSize(newSize);
				}
			} else {
				fileInfo.setCreatedSize(fileSize);
			}
			
			if (!torrent_.isWorking()) {
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.v(LOG_TAG, e.getMessage());
		} finally {
			if (file != null) {
				try {
					file.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/** Returns the size of free space in bytes. */
	public static long freeSize(String path) {
		try {
			StatFs fs = new StatFs(/*"/sdcard/"); //*/ path);
			return (long) fs.getAvailableBlocks() * fs.getBlockSize();
		} catch (Exception e) {
			return 5 * 1024L * 1024L * 1024L;
		}
	}

	/** Returns whether or not the file already exists. */
	public static boolean fileExists(String filepath) {
		return (new File(filepath)).exists();
	}
	
	/** Returns the size of the file. */
	public static long getFileSize(String filePath) {
		return (new File(filePath)).length();
	}

	/** Creates a new file and reserves free space for it. */
	/*public static boolean createFile(String filePath, long fileSize) {
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
	}*/
	
	/** Returns the content of the file in a byte array. */
	public static byte[] readFile(final String filePath) {
		BufferedInputStream bis = null;
		try {
			final File file = new File(filePath);
			bis = new BufferedInputStream(new FileInputStream(file));
			final byte[] bf = new byte[(int) file.length()];
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
	public int writeFile(final String filePath, final long filePosition, final byte[] block) {
		return writeFile(filePath, filePosition, block, 0, block.length);
	}

	/** 
	 * Writes a block to the file in the given position.
	 * The offset and length within the block is also given. 
	 */
	public int writeFile(final String filePath, final long filePosition, final byte[] block, final int offset, final int length) {
		RandomAccessFile file = null;
		try {
			if (!fileExists(filePath)) {
				final String dirPath = filePath.substring(0, filePath.lastIndexOf('/'));
				final File dir = new File(dirPath);
				dir.mkdirs();
			}
			
			file = new RandomAccessFile(filePath, "rw");
			file.seek(filePosition);
			file.write(block, offset, length);
			return Torrent.ERROR_NONE;
		} catch (IOException e) {
			Log.v(LOG_TAG, e.getMessage());
			return Torrent.ERROR_GENERAL;
		} finally {
			try {
				file.close();
			} catch (Exception e) {}
		}
	}
	
	/** 
	 * Reads a block from the file.
	 * The position and the length within the file is also given. 
	 */
	public byte[] read(String filepath, long position, int length) {
		RandomAccessFile file = null;
		final byte[] result = new byte[length];
		try {
			file = new RandomAccessFile(filepath, "r");
			file.seek(position);
			final int cnt = file.read(result);

			if (cnt != length) {
				//result = null;
				return null;
			}
		} catch (IOException e) {
			Log.v(LOG_TAG, e.getMessage());
			//result = null;
			return null;
		} finally {
			try {
				file.close();
			} catch (Exception e) {
			}
		}
		return result;
	}
	
}
