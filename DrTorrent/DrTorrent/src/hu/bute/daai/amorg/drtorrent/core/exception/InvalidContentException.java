package hu.bute.daai.amorg.drtorrent.core.exception;

public class InvalidContentException extends DrTorrentException {

	private static final long serialVersionUID = 1L;

	public InvalidContentException(String message) {
		super(message);
	}
	
	public InvalidContentException(String message, Throwable innerException) {
		super(message, innerException);
	}
}
