package hu.bute.daai.amorg.drtorrent.file;

import hu.bute.daai.amorg.drtorrent.core.Piece;
import hu.bute.daai.amorg.drtorrent.core.exception.DrTorrentException;
import hu.bute.daai.amorg.drtorrent.core.torrent.TorrentInfo;
import hu.bute.daai.amorg.drtorrent.util.Log;
import hu.bute.daai.amorg.drtorrent.util.sha1.SHA1;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;

import android.os.StatFs;

/** Class managing the file system. */
public class FileManager {
	private final static String LOG_TAG = "FileManager";
	
	final private TorrentInfo torrent_;
	final private Vector<hu.bute.daai.amorg.drtorrent.core.File> filesCreated_;
	final private Vector<hu.bute.daai.amorg.drtorrent.core.File> filesToCreate_;

	private boolean isCreating_ = false;
	
	public FileManager(final TorrentInfo torrent) {
		torrent_ = torrent;
		filesToCreate_ = new Vector<hu.bute.daai.amorg.drtorrent.core.File>();
		filesCreated_ = new Vector<hu.bute.daai.amorg.drtorrent.core.File>();
	}	
	
	/** Adds a new file to create. Returns true if it has not been added yet. */
	public boolean addFile(final hu.bute.daai.amorg.drtorrent.core.File file) {
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
			hu.bute.daai.amorg.drtorrent.core.File fileInfo = filesToCreate_.elementAt(i);
			createFile(fileInfo);
			
			if (fileInfo.getSize() == fileInfo.getCreatedSize()) {
				filesCreated_.addElement(fileInfo);
				if (filesToCreate_.removeElement(fileInfo)) {
					i--;
				}
			}
		}
		
		isCreating_ = false;
	}
	
	public void createFile(final hu.bute.daai.amorg.drtorrent.core.File fileInfo) {
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
				for (long size = file.length(); size < fileSize && torrent_.isWorking(); size += Piece.MAX_PIECE_SIZE_TO_READ_AT_ONCE) {
					long newSize = size + Piece.MAX_PIECE_SIZE_TO_READ_AT_ONCE; 
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
	
	/** Removes the file at the given path. */
	public static void removeFile(final String path) {
		File file = new File(path);
	    file.delete();
	}
	
	/** Removes the folder and its folders (but the files not!!!) at the given path. */
	public static void removeDirectories(final File dir) {
		if (dir.isDirectory()) {
	        for (File child : dir.listFiles()) {
	        	removeDirectories(child);
	        }
	        dir.delete();
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
	
	/** Returns the content of the file in a byte array. */
	public static byte[] read(final String filePath) {
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
	
	/** Writes a block to the file in the given position.  */
	public static void write(final String filePath, final long filePosition, final byte[] block) throws DrTorrentException {
		write(filePath, filePosition, block, 0, block.length);
	}

	/** 
	 * Writes a block to the file in the given position.
	 * The offset and length within the block is also given. 
	 */
	public static void write(final String filePath, final long filePosition, final byte[] block, final int offset, final int length) throws DrTorrentException {
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
		} catch (IOException e) {
			Log.v(LOG_TAG, e.getMessage());
			throw new DrTorrentException("Could not write the file.", e);
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
	public static byte[] read(final String filepath, final long position, final int length) {
		RandomAccessFile file = null;
		final byte[] result = new byte[length];
		try {
			file = new RandomAccessFile(filepath, "r");
			file.seek(position);
			final int cnt = file.read(result);

			if (cnt != length) {
				return null;
			}
		} catch (IOException e) {
			Log.v(LOG_TAG, e.getMessage());
			return null;
		} finally {
			try {
				file.close();
			} catch (Exception e) {
			}
		}
		return result;
	}
	
	/** Reading from 'filepath' in 'position' to 'result'. */
	public static int read(final String filepath, final long position, final byte[] result) {
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(filepath, "r");
			file.seek(position);
			return file.read(result);
		} catch (IOException e) {
			Log.v(LOG_TAG, e.getMessage());
			return 0;
		} finally {
			try {
				file.close();
			} catch (Exception e) {
			}
		}
	}
	
	/** Reading from 'filepath' in 'position' 'length' data to 'sha1'. */
	public static void read(final String filepath, final long position, final int length, final SHA1 sha1) {
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(filepath, "r");
			
			byte[] data = new byte[Piece.MAX_PIECE_SIZE_TO_READ_AT_ONCE];
			int offset = 0;
			while (offset < length) {
				int nextLength = Piece.MAX_PIECE_SIZE_TO_READ_AT_ONCE;
				if (offset + nextLength > length) {
					nextLength = length - offset;
					data = new byte[nextLength];
				}
				file.seek(position + offset);
				file.read(data);
				sha1.update(data);
				
				offset += nextLength;
			}
			data = null;

		} catch (IOException e) {
			Log.v(LOG_TAG, e.getMessage());
		} finally {
			try {
				file.close();
			} catch (Exception e) {
			}
		}
	}
	
}
