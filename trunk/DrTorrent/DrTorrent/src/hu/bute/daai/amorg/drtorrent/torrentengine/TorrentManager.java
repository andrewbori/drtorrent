package hu.bute.daai.amorg.drtorrent.torrentengine;

import hu.bute.daai.amorg.drtorrent.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.file.FileManager;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;

import java.util.Vector;

import android.os.Bundle;
import android.os.Message;

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
		{
			torrents_.add(newTorrent);
			return newTorrent;
		}

		return null;
	}
	
	/** Starts the torrent with its given info hash. */
	public void startTorrent(String infoHash) {
		Torrent torrent = getTorrent(infoHash);
		torrent.start();
	}

	/** Returns whether the manager the given torrent already has. */
	public boolean hasTorrent(Torrent torrent) {
		if (getTorrent(torrent.getInfoHash()) != null) return true;
		
		return false;
	}
	
	/** Returns whether the manager the given torrent already has. */
	public Torrent getTorrent(String infoHash) {
		Torrent tempTorrent;
		String tempInfoHash;
		for (int i = 0; i < torrents_.size(); i++) {
			tempTorrent = (Torrent) torrents_.elementAt(i);
			tempInfoHash = tempTorrent.getInfoHash();
			if (tempInfoHash != null && tempInfoHash.equals(infoHash)) {
				return tempTorrent;
			}
		}

		return null;
	}
	
	public void changedTorrent(String infoHash) {
		torrentService_.changedTorrent(infoHash);
		
		/*Message msg = Message.obtain();
		Bundle b = new Bundle();
		b.putString(TorrentService.MSG_KEY_TORRENT_INFOHASH, infoHash);
		msg.what = TorrentService.MSG_TORRENT_CHANGED;
		*/
	}
	
	public String getPeerID() {
		return "-DR0001-xlccrlbsjlse";
	}

}
