package hu.bute.daai.amorg.drtorrent.core.torrent;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.core.DownloadManager;
import hu.bute.daai.amorg.drtorrent.core.UploadManager;
import hu.bute.daai.amorg.drtorrent.file.FileComparator;
import hu.bute.daai.amorg.drtorrent.file.FileManager;
import hu.bute.daai.amorg.drtorrent.util.Preferences;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedDictionary;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedList;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedString;
import hu.bute.daai.amorg.drtorrent.util.sha1.SHA1;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/** Class that represents a torrent that is creating. */
public class CreatingTorrent extends Torrent {

	private String torrentPath_;
	private boolean isEnabled_ = true;
	
	private String filePath_ = null;
	private String trackersStr_ = null;
	private boolean isPrivate_ = false;
	
	/** */
	public CreatingTorrent(TorrentManager torrentManager,
	UploadManager uploadManager, DownloadManager downloadManager,
	String filePath, String downloadPath) {
		super(torrentManager, uploadManager, downloadManager, filePath, downloadPath);
		
		status_ = STATUS_CREATING;
		isEnabled_ = true;
	}
	
	@Override
	public void start() {
		status_ = STATUS_CREATING;
		isEnabled_ = true;
		create(filePath_, trackersStr_, isPrivate_, torrentPath_, shouldStart_);
	}
	
	@Override
	public void resume() {
		
	}
	
	@Override
	public void stop() {
		isEnabled_ = false;
		status_ = STATUS_STOPPED_CREATING;
	}
	
	@Override
	public void onTimer() {
		
	}
	
	public void setName(String filePathSaveAs) {
		int index = filePathSaveAs.lastIndexOf('/');
		if (index >= 0 && filePathSaveAs.length() > index + 1) {
			super.name_ = filePathSaveAs.substring(index + 1);
		} else {
			super.name_ = filePathSaveAs;
		}
	}
	
	/** Starts creating the torrent. */
	protected void create(String filePath, String trackersStr, boolean isPrivate, String filePathSaveAs, boolean shouldStart) {
		filePath_ = filePath;
		trackersStr_= trackersStr;
		isPrivate_ = isPrivate;
				
		File file = new File(filePath);
		downloadFolder_ = file.getParent();
		torrentPath_ = filePathSaveAs;
		shouldStart_ = shouldStart;
		
		// Create announce list
		String announce = null;
		BencodedList announceList = new BencodedList();
		String[] trackers = trackersStr.split("\n");
		for (String tracker : trackers) {
			if (!tracker.equals("")) {
				BencodedList trackersSubList = new BencodedList();
				trackersSubList.append(new BencodedString(tracker));
				if (announce == null) {
					announce = tracker;
				}
				
				announceList.append(trackersSubList);
			}
		}
		
		// Get torrent name
		File dirOrFile = new File(filePath);
		super.name_ = dirOrFile.getName();
		
		// Calculate torrent size
		if (dirOrFile.isDirectory()) {
			super.activeSize_ = 0;
			processDir(dirOrFile, new BencodedList(), false);
			if (!isEnabled_) {
				reset();
				return;
			}
		} else {
			super.activeSize_ = dirOrFile.length();
		}
		
		// Calculate piece length
		long pieceLengthEstimator = super.activeSize_ / 1024;	// No more pieces than 1024
		
		pieceLength_ = 16 * 1024;	// 16 kB
		while (pieceLengthEstimator > pieceLength_ && pieceLength_ < 16 * 1024 * 1024) {
			pieceLength_ *= 2;
		}
		
		// Calculate every piece hash
		if (dirOrFile.isDirectory()) {
			fileList_ = new BencodedList();
			baosPieces_ = new ByteArrayOutputStream();
			processDir(dirOrFile, new BencodedList(), true);
			if (!isEnabled_) {
				reset();
				return;
			}
		} else {
			baosPieces_ = new ByteArrayOutputStream();
			calculateSHA1(dirOrFile);
			if (!isEnabled_) {
				return;
			}
			calculateSHA1Finished();
		}
		
		// Create torrent
		BencodedDictionary info = new BencodedDictionary();
		if (isPrivate) {
			info.addEntry("private", 1);
		}
		info.addEntry("name", super.name_);
		if (dirOrFile.isDirectory()) {
			info.addEntry("files", fileList_);
		} else {
			info.addEntry("length", super.activeSize_);
		}
		
		info.addEntry("piece length", pieceLength_);
		info.addEntry("pieces", new BencodedString(baosPieces_.toByteArray()));
		
		BencodedDictionary torrent = new BencodedDictionary();
		if (announce != null) {
			torrent.addEntry("announce", announce);
		}
		if (announceList.count() > 1) {
			torrent.addEntry("announce-list", announceList);
		}
		//torrent.addEntry("comment", "Something");
		torrent.addEntry("created by", "DrTorrent " + Preferences.APP_VERSION_NAME);
		torrent.addEntry("creation date", System.currentTimeMillis() / 1000L);
		torrent.addEntry("info", info);
		
		FileManager.removeFile(filePathSaveAs);
		FileManager.write(filePathSaveAs, 0, torrent.Bencode());
		
		// Open torrent
		torrentManager_.openCreatedTorrent(this);
	}
	
	private BencodedList fileList_;
	private int pieceLength_;
	private final SHA1 sha1_ = new SHA1();
	private int sha1Length_ = 0;
	private ByteArrayOutputStream baosPieces_;
	
	/** Builds the Bencoded file list of the given directory. */
	private void processDir(File dir, BencodedList currentPath, boolean shouldBuild) {
		processDir(dir, currentPath, shouldBuild, true);
	}
	
	/** Builds the Bencoded file list of the given directory. */
	private void processDir(File dir, BencodedList currentPath, boolean shouldBuild, boolean isRootDir) {
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		Arrays.sort(files, new FileComparator());
		
		for (File file : files) {
			if (file.isFile()) {
				if (shouldBuild) {
					BencodedDictionary fileDict = new BencodedDictionary();
					fileDict.addEntry("length", file.length());
					
					BencodedList path = new BencodedList(currentPath);
					path.append(new BencodedString(file.getName()));
					fileDict.addEntry("path", path);
				
					fileList_.append(fileDict);
					
					calculateSHA1(file);
					if (!isEnabled_) {
						return;
					}
				} else {
					super.activeSize_ += file.length();
				}
				
			
			} else if (file.isDirectory()) {
				BencodedList path = new BencodedList(currentPath);
				path.append(new BencodedString(file.getName()));
				processDir(file, path, shouldBuild, false);
				if (!isEnabled_) {
					return;
				}
			}
		}
		
		if (shouldBuild && isRootDir) {
			calculateSHA1Finished();
		}
	}
	
	/** Calculate the SHA1 checksums of the file. Puts the result to baosPieces_. */
	private void calculateSHA1(File file) {
		long length = file.length();
		for (long processedSize = 0; processedSize < length;) {
			long currentPos = processedSize;
			
			int nextSize = pieceLength_ - sha1Length_;
			if (processedSize + nextSize > length) {
				nextSize = (int) (length - processedSize);
			}
			sha1Length_ += nextSize;
			
			processedSize += nextSize;
			super.activeDownloadedSize_ += nextSize;

			FileManager.read(file.getPath(), currentPos, nextSize, sha1_);
			
			if (sha1Length_ == pieceLength_) {
				try {
					baosPieces_.write(SHA1.resultToByte(sha1_.digest()));
				} catch (IOException e) {
					e.printStackTrace();
				}
				sha1_.reset();
				sha1Length_ = 0;
			}
			if (!isEnabled_) {
				return;
			}
		}
	}
	
	/** Should call when finishing calculating to append the hash of the leftover data. */
	private void calculateSHA1Finished() {
		if (sha1Length_ > 0) {
			try {
				baosPieces_.write(SHA1.resultToByte(sha1_.digest()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			sha1_.reset();
			sha1Length_ = 0;
		}
	}
	
	@Override
	public double getProgressPercent() {
		if (status_ == R.string.status_creating) {
			return super.activeDownloadedSize_ * 100.0 / (double)super.activeSize_;
		}
		return super.getProgressPercent();
	}
	
	/** Returns the path of the created torrent. */
	public String getTorrentPath() {
		return torrentPath_;
	}
	
	private void reset() {
		fileList_ = null;
		pieceLength_ = 0;
		sha1_.reset();
		sha1Length_ = 0;
		baosPieces_ = null;
		super.activeDownloadedSize_ = 0;
	}
}
