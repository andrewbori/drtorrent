package hu.bute.daai.amorg.drtorrent.util;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.R.string;

import java.io.Serializable;
import java.util.ArrayList;

import android.content.Context;

public class Time implements Serializable {
	private static final long serialVersionUID = -6306819583089434813L;
	
	public final static int MSEC = 1;
	public final static int SEC  = 2;
	public final static int MIN  = 3;
	public final static int HOUR = 4;
	public final static int DAY  = 5;
	public final static int YEAR = 6;
	
	private ArrayList<Integer> metric_ = null;
	private ArrayList<Integer> unit_ = null;
	
	/** 
	 * Constructor of the time.
	 *
	 * @param time      the integer value to be converted
	 * @param unit      the unit of the time
	 * @param precision the precision of the returned text
	 */
	public Time(long time, int unit, int precision) {
		metric_ = new ArrayList<Integer>();
		unit_ = new ArrayList<Integer>();
		
		set(time, unit, precision);
	}
	
	/** Constructor of an empty time. */
	public Time() {
		
	}
	
	/** 
	 * Converts the given time to a human readable text.
	 *
	 * @param time      the integer value to be converted
	 * @param unit      the unit of the time
	 * @param precision the precision of the returned text
	 */
	private void set(long time, int unit, int precision) {
		if (time == -1 || precision <= 0) {
			return ;
		}
		
		long timeTemp = time;
		ArrayList<Integer> times = new ArrayList<Integer>();
		
		switch (unit) {
			case MSEC:
				timeTemp /= 1000;
				time = timeTemp;
				
			case SEC:
				unit = SEC;
				if (timeTemp >= 60) {
					timeTemp /= 60;
					times.add(0, (int) (time - (timeTemp * 60)));
					time = timeTemp;
				} else break;
				
			case MIN:
				unit = MIN;
				if (timeTemp >= 60) {
					timeTemp /= 60;
					times.add(0, (int) (time - (timeTemp * 60)));
					time = timeTemp;
				} else break;
				
			case HOUR:
				unit = HOUR;
				if (timeTemp >= 24) {
					timeTemp /= 24;
					times.add(0, (int) (time - (timeTemp * 24)));
					time = timeTemp;
				} else break;
				
			case DAY:
				unit = DAY;
				if (timeTemp >= 365) {
					timeTemp /= 365;
					times.add(0, (int) (time - (timeTemp * 365)));
					time = timeTemp;
				} else break;
				
			case YEAR:
				unit = YEAR;
		}
		times.add(0, (int) time);
		
		for (int i = 0; i < times.size() && precision > 0 && unit > MSEC; i++, precision--, unit--) {
			metric_.add(times.get(i));
			
			switch (unit) {
				case SEC:  unit_.add(R.string.time_sec); break;
				case MIN:  unit_.add(R.string.time_min); break;
				case HOUR: unit_.add(R.string.time_hour); break;
				case DAY:  unit_.add(R.string.time_day); break;
				case YEAR: unit_.add(R.string.time_year); break;
			}
		}
	}
	
	public String toString(Context context) {
		if (metric_ == null) {
			return "";
		}
		
		if (metric_.isEmpty()) {
			return context.getString(R.string.infinity);
		}
		
		String timeStr = "";
		for (int i = 0; i < unit_.size(); i++) {
			timeStr += metric_.get(i) + context.getString(unit_.get(i));
		}
		return timeStr;
	}
	
	@Override
	public String toString() {
		if (metric_ == null) {
			return "";
		}
		
		if (metric_.isEmpty()) {
			return "???";
		}
		
		String timeStr = "";
		for (int i = 0; i < unit_.size(); i++) {
			timeStr += metric_.get(i);
			switch (unit_.get(i)) {
				case R.string.time_sec: timeStr += " s "; break;
				case R.string.time_min: timeStr += " m "; break;
				case R.string.time_hour: timeStr += " h "; break;
				case R.string.time_day: timeStr += " d "; break;
				case R.string.time_year: timeStr += " y "; break;
			}
		}
		return timeStr;
	}
}
