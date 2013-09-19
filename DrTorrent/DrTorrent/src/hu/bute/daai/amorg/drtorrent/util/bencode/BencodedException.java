package hu.bute.daai.amorg.drtorrent.util.bencode;

public class BencodedException extends Exception {
	private static final long serialVersionUID = 1L;

	public BencodedException(String message) {
		super(message);
	}
	
	public BencodedException(String message, Throwable innerException) {
		super(message, innerException);
	}
}
