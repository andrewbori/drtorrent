package hu.bute.daai.amorg.drtorrent.core.exception;

public class NotEnoughFreeSpaceException extends DrTorrentException {

	private static final long serialVersionUID = 1L;

	public NotEnoughFreeSpaceException(String message) {
		super(message);
	}
	
	public NotEnoughFreeSpaceException(String message, Throwable innerException) {
		super(message, innerException);
	}
}