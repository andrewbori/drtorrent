package hu.bute.daai.amorg.drtorrent.core.exception;

public class DrTorrentException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public DrTorrentException(String message) {
		super(message);
	}
	
	public DrTorrentException(String message, Throwable innerException) {
		super(message, innerException);
	}
}
