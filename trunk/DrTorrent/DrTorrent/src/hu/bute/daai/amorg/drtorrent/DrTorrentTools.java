package hu.bute.daai.amorg.drtorrent;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class DrTorrentTools {

	public static boolean byteArrayEqual(byte[] array1, byte[] array2) {
		if (array1.length != array2.length)
			return false;

		for (int i = 0; i < array1.length; i++) {
			if (array1[i] != array2[i])
				return false;
		}

		return true;
	}
	
	/** Converts the given amount of bytes to a human readable text. */
	public static String bytesToString(long bytesLong) {
		double bytes = bytesLong;
		String metric = " B";
		if (bytes > 1023.0) {
			bytes = bytes / 1024.0;
			metric = " kB";
		
			if (bytes > 1023.0) {
					bytes = bytes / 1024.0;
					metric = " MB";
					
				if (bytes > 1023.0) {
					bytes = bytes / 1024.0;
					metric = " GB";
					
					if (bytes > 1023.0) {
						bytes = bytes / 1024.0;
						metric = " TB";
					}
				}
			}
		}
		DecimalFormat dec = new DecimalFormat("###.#");

		return dec.format(bytes) + metric;
	}
	
	public final static int MSEC = 1;
	public final static int SEC  = 2;
	public final static int MIN  = 3;
	public final static int HOUR = 4;
	public final static int DAY  = 5;
	public final static int YEAR = 6;
	
	/** 
	 * Converts the given time to a human readable text.
	 *
	 * @param time      the integer value to be converted
	 * @param metric    the metric (unit) of the time
	 * @param precision the precision of the returned text
	 */
	public static String timeToString(long time, int metric, int precision) {
		if (time == -1) return "???";
		if (precision <= 0) return "";
		
		long timeTemp = time;
		ArrayList<Integer> times = new ArrayList<Integer>();
		
		switch (metric) {
			case MSEC:
				timeTemp /= 1000;
				time = timeTemp;
				
			case SEC:
				metric = SEC;
				if (timeTemp >= 60) {
					timeTemp /= 60;
					times.add(0, (int) (time - (timeTemp * 60)));
					time = timeTemp;
				} else break;
				
			case MIN:
				metric = MIN;
				if (timeTemp >= 60) {
					timeTemp /= 60;
					times.add(0, (int) (time - (timeTemp * 60)));
					time = timeTemp;
				} else break;
				
			case HOUR:
				metric = HOUR;
				if (timeTemp >= 24) {
					timeTemp /= 24;
					times.add(0, (int) (time - (timeTemp * 24)));
					time = timeTemp;
				} else break;
				
			case DAY:
				metric = DAY;
				if (timeTemp >= 365) {
					timeTemp /= 365;
					times.add(0, (int) (time - (timeTemp * 365)));
					time = timeTemp;
				} else break;
				
			case YEAR:
				metric = YEAR;
		}
		times.add(0, (int) time);
		
		StringBuilder timeStr = new StringBuilder();
		for (int i = 0; i < times.size() && precision > 0 && metric > MSEC; i++, precision--, metric--) {
			if (i > 0) timeStr.append(" ");
			timeStr.append(times.get(i));
			
			switch (metric) {
				case SEC:  timeStr.append(" s"); break;
				case MIN:  timeStr.append(" m"); break;
				case HOUR: timeStr.append(" h"); break;
				case DAY:  timeStr.append(" d"); break;
				case YEAR: timeStr.append(" y"); break;
			}
		}

		return timeStr.toString();
	}
}
