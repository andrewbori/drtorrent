package hu.bute.daai.amorg.drtorrent.core.exception;

public class AlreadyExistsException extends DrTorrentException {

	private static final long serialVersionUID = 1L;

	public AlreadyExistsException(String message) {
		super(message);
	}
	
	public AlreadyExistsException(String message, Throwable innerException) {
		super(message, innerException);
	}
}