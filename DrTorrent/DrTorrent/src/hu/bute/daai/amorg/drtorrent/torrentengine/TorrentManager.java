package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.file.FileManager;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;

import java.util.Vector;

/** Class managing the opened torrents. */
public class TorrentManager {
	public static String DEFAULT_DOWNLOAD_PATH = "/sdcard/Downloads/";
	
	private TorrentService torrentService_;
	private Vector<Torrent> torrents_;

	public TorrentManager(TorrentService torrentService) {
		this.torrentService_ = torrentService;
		torrents_ = new Vector<Torrent>();
	}

	/** Opens a new Torrent file with the given file path. */
	public Torrent openTorrent(String filePath) {
		return openTorrent(filePath, false, DEFAULT_DOWNLOAD_PATH);
	}
	
	/**
	 * Opens a new Torrent file with the given file path.
	 * Also defines its download folder and whether it was previously saved.   
	 */
	public Torrent openTorrent(String filePath, boolean isSaved, String downloadPath) {
		byte[] fileContent = FileManager.readFile(filePath);

		if (fileContent == null) {
			return null;
		}

		Bencoded bencoded = Bencoded.parse(fileContent);

		if (bencoded == null) {
			return null;
		}

		Torrent newTorrent = new Torrent(this);

		if (newTorrent.set(bencoded, isSaved) == Torrent.ERROR_NONE)
			return newTorrent;

		return null;
	}

	/** Returns whether the manager the given torrent already has. */
	public boolean hasTorrent(Torrent torrent) {
		String infoHash = torrent.getInfoHash();
		Torrent tempTorrent;
		String tempInfoHash;
		for (int i = 0; i < torrents_.size(); i++) {
			tempTorrent = (Torrent) torrents_.elementAt(i);
			tempInfoHash = tempTorrent.getInfoHash();
			if (tempInfoHash != null && tempInfoHash.equals(infoHash)) {
				return true;
			}
		}

		return false;
	}

}
