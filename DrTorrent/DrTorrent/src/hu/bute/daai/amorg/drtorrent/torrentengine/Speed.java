package hu.bute.daai.amorg.drtorrent.torrentengine;

import java.util.Vector;

/** Class for download/upload speed measurement. */
public class Speed {
	private static int TIME_INTERVAL = 10;
	
	private Vector<Integer> bytes_ = null;
	private Vector<Long> seconds_ = null;
	
	/** Constructor. */
	public Speed() {
		clean();
	}
	
	/** Adds bytes to the speed. */
	public void addBytes(int bytes, long time) {
		time /= 1000;
		synchronized (bytes_) {
			synchronized (seconds_) {
				if (seconds_.size() < 1 || seconds_.lastElement() < time) {
					bytes_.add(bytes);
					seconds_.add(time);
					
					if (seconds_.lastElement() - seconds_.firstElement() > TIME_INTERVAL) {
						bytes_.removeElementAt(0);
						seconds_.removeElementAt(0);
					}
				} else {
					bytes_.set(bytes_.size() - 1, bytes_.lastElement() + bytes);
				}
			}
		}
	}
	
	/** Returns the speed in bytes/second. */
	public int getSpeed() {
		if (seconds_.size() > 0) {
			int bytes = 0;
			for (int i = 0; i < bytes_.size(); i++) {
				bytes += bytes_.elementAt(i);
			}
			return bytes / (int) (seconds_.lastElement() - seconds_.firstElement() + 1);
		}
		
		return 0;
	}
	
	/** Returns the bytes. */
	public int getBytes() {
		int bytes = 0;
		for (int i = 0; i < bytes_.size(); i++) {
			bytes += bytes_.elementAt(i);
		}
		return bytes;
	}
	
	/** Reinitializes the speed. */
	public void clean() {
		bytes_ = new Vector<Integer>();
		seconds_ = new Vector<Long>();
	}
}