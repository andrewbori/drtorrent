package hu.bute.daai.amorg.drtorrent;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class Tools {

	/** Reads an (size-byte) integer value from a byte array at the given position. */
	private static long readLong(byte[] array, int position, int size) {
		long num = array[position];
		if (num < 0) num += 256;
		for (int i = 1; i < size; i++) {
			num <<= 8;
			num += array[position + i];
			if (array[position + i] < 0) num += 256;
		}
		return num;
	}
	
	/** Reads a 16-bit integer from a byte array at the given position. */
	public static int readInt16(byte[] array, int position) {
		return (int) readLong(array, position, 2);
	}
	
	/** Reads a 32-bit integer from a byte array at the given position. */
	public static int readInt32(byte[] array, int position) {
		return (int) readLong(array, position, 4);
	}
	
	/** Reads a 64-bit integer from a byte array at the given position. */
	public static long readInt64(byte[] array, int position) {
		return (int) readLong(array, position, 8);
	}
	
    /** Converts a byte array (four byte big-endian value) to an integer value. */
    public static int byteArrayToInt32(byte[] array) {
        return (array[0] << 24) +
              ((array[1] & 0xFF) << 16) +
              ((array[2] & 0xFF) << 8) +
               (array[3] & 0xFF);
    }
	
	/** Converts a 64 bit-integer value to a byte array (16 byte big-endian value). */
	public static byte[] int64ToByteArray(long value) {
		return new byte[] {
				(byte)(value >>> 56),
		        (byte)(value >>> 48),
		        (byte)(value >>> 40),
		        (byte)(value >>> 32),
		        (byte)(value >>> 24),
		        (byte)(value >>> 16),
		        (byte)(value >>> 8),
		        (byte) value
	        };
	}
	
    /** Converts a 32 bit-integer value to a byte array (8 byte big-endian value). */
	public static byte[] int32ToByteArray(int value) {
        return new byte[] {
	        (byte)(value >>> 24),
	        (byte)(value >>> 16),
	        (byte)(value >>> 8),
	        (byte) value
        };
	}
    
    /** Converts a 16 bit-integer value to byte array (4 byte big-endian value). */
	public static byte[] int16ToByteArray(int value) {
        return new byte[] {
	        (byte)(value >>> 8),
	        (byte) value
        };
	}
	
	/** Reads an IP address from a byte array at the given position. */
	public static String readIp(byte[] array, int position) {
		if (array.length < position + 4) return null;
		
		int a, b, c, d;                  
        if (array[position] < 0) 	 a = 256 + array[position];
        else              	      	 a = array[position];
        if (array[position + 1] < 0) b = 256 + array[position + 1];
        else              		 	 b = array[position + 1];
        if (array[position + 2] < 0) c = 256 + array[position + 2];
        else              		 	 c = array[position + 2];
        if (array[position + 3] < 0) d = 256 + array[position + 3];
        else              			 d = array[position + 3];
         
        if (a == 0 && b == 0 && c == 0 && d == 0) return null;
        return a + "." + b + "." + c + "." + d;
	}
	
	/** Returns whether the byte arrays equal or not. */
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
