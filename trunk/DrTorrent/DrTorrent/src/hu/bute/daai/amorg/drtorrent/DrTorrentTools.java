package hu.bute.daai.amorg.drtorrent;

import java.text.DecimalFormat;

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
	
	public static String bytesToString(int bytesInt) {
		double bytes = bytesInt;
		String bytesStr = "";
		if (bytes > 1024.0) {
			bytes = bytes / 1024.0;
			String metric = "kB";
		
			if (bytes > 1024.0) {
					bytes = bytes / 1024.0;
					metric = "MB";
					
				if (bytes > 1024.0) {
					bytes = bytes / 1024.0;
					metric = "GB";
				}
			}
			DecimalFormat dec = new DecimalFormat("###.#");
			bytesStr = dec.format(bytes) + " " + metric;
		} else bytesStr = (int) bytes + " byte";
		
		return bytesStr;
	}
	
	public final static int MSEC = 1;
	public final static int SEC  = 2;
	public final static int MIN  = 3;
	public final static int HOUR = 4;
	public final static int DAY  = 5;
	public final static int YEAR = 6;
	
	public static String intToTime(int timeInt, int metric, int precision) {
		if (timeInt == -1) return "???";
		if (precision == 0) return "";
		
		double time = (double) timeInt;
		
		String metricStr = "";
		if (metric == MSEC) {
			time /= 1000.0;
			metric = SEC;
			metricStr = "s";
		}
		
		switch (metric) {			
			case SEC:
				if (time >= 60.0) {
					time /= 60.0;
					metricStr = "m";
					metric = MIN;
				} else {
					return ((int) time) + " s";
				}
				
			case MIN:
				if (time >= 60.0) {
					time /= 60.0;
					metricStr = "h";
					metric = HOUR;
				} else break;
				
			case HOUR:
				if (time >= 24.0) {
					time /= 24.0;
					metricStr = "d";
					metric = DAY;
				} else break;
				
			case DAY:
				if (time >= 365.0) {
					time /= 365.0;
					metricStr = "y";
					metric = YEAR;
				} else break;
		}
		
		double nextTime = 0.0;
		precision--;
		if (precision > 0) {
			timeInt = (int) time;
			nextTime = time - (double) timeInt;
			switch (metric) {			
				case MIN:
					nextTime *= 60.0;
					break;
					
				case HOUR:
					nextTime *= 60.0;
					break;
					
				case DAY:
					nextTime *= 24.0;
					break;
					
				case YEAR:
					nextTime *= 365.0;
					break;
					
				default:
					break;
			}
		}
		
		String timeStr = "";
		
		timeStr = (int) time + " " + metricStr + " " + intToTime((int) nextTime, metric - 1, precision);
		
		return timeStr;
	}
	
	public static String millisToTime(int millisInt) {
		if (millisInt == -1) return "???";
		double millis = millisInt;
		double time = millis / 1000.0;
		String timeStr = "";
		if (time > 59.0) {
			time /= 60.0;
			String metric = "m";
		
			if (time > 59.0) {
					time /= 60.0;
					metric = "h";
					
				if (time > 23.0) {
					time /= 24.0;
					metric = "d";
					
					if (time > 364.0) {
						time /= 365.0;
						metric = "y";
					}
				}
			}
			DecimalFormat dec = new DecimalFormat("###.#");
			timeStr = dec.format(time) + " " + metric;
		} else timeStr = (int) time + " s";
		
		return timeStr;
	}

}
