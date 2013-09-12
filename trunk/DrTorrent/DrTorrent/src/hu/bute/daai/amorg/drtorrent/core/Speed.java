package hu.bute.daai.amorg.drtorrent.core;

import java.util.Vector;

/** Class for download/upload speed measurement. */
public class Speed {
	public static int TIME_INTERVAL = 7;
	
	final private Vector<Long> bytes_;
	final private Vector<Long> seconds_;
	
	/** Constructor. */
	public Speed() {
		bytes_ = new Vector<Long>();
		seconds_ = new Vector<Long>();
	}
	
	/** Adds bytes to the speed. */
	public synchronized void addBytes(long bytes, long time) {
		time /= 1000;
		if (seconds_.size() < 1 || seconds_.lastElement() < time) {
			bytes_.addElement(bytes);
			seconds_.addElement(time);
			
			if (seconds_.lastElement() - seconds_.firstElement() > TIME_INTERVAL) {
				bytes_.removeElementAt(0);
				seconds_.removeElementAt(0);
			}
		} else {
			bytes_.set(bytes_.size() - 1, bytes_.lastElement() + bytes);
		}
	}
	
	/** Returns the speed in bytes/second. */
	public int getSpeed() {
		if (seconds_.size() > 0) {
			long bytes = 0;
			for (int i = 0; i < bytes_.size(); i++) {
				bytes += bytes_.elementAt(i);
			}
			return (int) (bytes / (int) (seconds_.lastElement() - seconds_.firstElement() + 1));
		}
		
		return 0;
	}
	
	/** Returns the bytes. */
	public long getBytes() {
		long bytes = 0;
		for (int i = 0; i < bytes_.size(); i++) {
			bytes += bytes_.elementAt(i);
		}
		return bytes;
	}
}
