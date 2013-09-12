package hu.bute.daai.amorg.drtorrent.util;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.R.string;

import java.io.Serializable;
import java.text.DecimalFormat;

import android.content.Context;

public class Quantity implements Serializable {
	private static final long serialVersionUID = -6907994830528202269L;
	
	public final static int SIZE  = 1;
	public final static int SPEED = 2;
	private static DecimalFormat dec = new DecimalFormat("###.#");
	
	private double metric_;
	private int unit_;
	
	public Quantity(long amount, int type) {
		switch (type) {
			case SIZE:
			case SPEED:
				bytesToString(amount, type==SIZE);
				break;
		}
	}

	/** Converts the given amount of bytes to a human readable text. */
	private void bytesToString(long bytesLong, boolean sizeOnly) {
		double metric = bytesLong;
		int unit = sizeOnly ? R.string.unit_b : R.string.unit_bps;
		if (metric > 999.0) {
			metric = metric / 1024.0;
			unit = sizeOnly ? R.string.unit_kb : R.string.unit_kbps;
		
			if (metric > 999.0) {
					metric = metric / 1024.0;
					unit = sizeOnly ? R.string.unit_mb : R.string.unit_mbps;
					
				if (metric > 999.0) {
					metric = metric / 1024.0;
					unit = sizeOnly ? R.string.unit_gb : R.string.unit_gbps;
					
					if (metric > 999.0) {
						metric = metric / 1024.0;
						unit = sizeOnly ? R.string.unit_tb : R.string.unit_tbps;
					}
				}
			}
		}
		
		metric_ = metric;
		unit_ = unit;
	}
	
	public double getMetric() {
		return metric_;
	}
	
	public int getUnit() {
		return unit_;
	}
	
	public String toString(Context context) {
		return dec.format(metric_) + " " + context.getString(unit_);
	}
	
	public String toString(Context context, int unit) {
		return (unit_ == unit) ? dec.format(metric_) : toString(context);
	}
	
	@Override
	public String toString() {
		String unit = "";
		switch (unit_) {
			case R.string.unit_b:
				unit = "B";
				break;
			case R.string.unit_kb:
				unit = "kB";
				break;
			case R.string.unit_mb:
				unit = "MB";
				break;
			case R.string.unit_gb:
				unit = "GB";
				break;
			case R.string.unit_tb:
				unit = "TB";
				break;
			case R.string.unit_bps:
				unit = "B/s";
				break;
			case R.string.unit_kbps:
				unit = "kB/s";
				break;
			case R.string.unit_mbps:
				unit = "MB/s";
				break;
			case R.string.unit_gbps:
				unit = "GB/s";
				break;
			case R.string.unit_tbps:
				unit = "TB/s";
				break;
			default:
				break;
		}
		return dec.format(metric_) + " " + unit;
	}
}
