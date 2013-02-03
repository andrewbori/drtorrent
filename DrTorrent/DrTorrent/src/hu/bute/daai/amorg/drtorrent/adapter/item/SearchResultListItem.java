package hu.bute.daai.amorg.drtorrent.adapter.item;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class SearchResultListItem {
	private String name_;
	private String torrentUrl_;
	private String detailUrl_;
	private String size_;
	private String added_;
	private String peers_;
	
	public SearchResultListItem(String name, String torrentUrl, String detailUrl, String size, String added, String seeds, String peers) {
		this.name_ = (name != null) ? name : "";
		this.torrentUrl_ = (torrentUrl != null) ? torrentUrl : "";
		this.detailUrl_ = (detailUrl != null) ? detailUrl : "";
		
		if (size != null) {
			size = size.replaceAll(" ", "");
			if (size.equals("")) {
				size = "???";
			} else {
				size = size.toLowerCase(Locale.ENGLISH);
				size = size.replace("kilobyte", "kB");
				size = size.replace("megabyte", "MB");
				size = size.replace("gigabyte", "GB");
				size = size.replace("terabyte", "TB");
				size = size.replace("byte", "B");
				size = size.replace("b", "B");
				size = size.replace("K", "k");
				size = size.replace("m", "M");
				size = size.replace("g", "G");
				size = size.replace("t", "T");
				size = size.replace("kiB", "kB");
				size = size.replace("MiB", "MB");
				size = size.replace("GiB", "GB");
				size = size.replace("TiB", "TB");
				
				int i = size.indexOf("B");
				if (size.length() > 0 && i > -1) {
					size = size.substring(0, i + 1);
				}
				
				i = 0;
				while (i < size.length() && ((size.charAt(i) >= '0' && size.charAt(i) <= '9') || size.charAt(i) == '.' || size.charAt(i) == ',')) {
					i++;
				}
				
				if (i < size.length()) {
					size = size.substring(0, i) + " " + size.substring(i, size.length());
				}
			}
		}
		this.size_ = (size != null) ? size : "";
		
		long dateLong = -1;
		try {
			dateLong = Long.parseLong(added);
		} catch (Exception e) {
		}
		if (dateLong > -1) {
			Date date = new Date(dateLong);
			DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
			
			this.added_ = dateFormat.format(date);
		} else {
			this.added_ = "???";
		}
		
		if (peers == null) {
			peers = "?";
		}
		if (seeds == null) {
			seeds = "?";
		}
		this.peers_ = seeds.concat("/").concat(peers);
	}
	
	public String getName() {
		return name_;
	}
	public String getTorrentUrl() {
		return torrentUrl_;
	}
	public String getDetailUrl() {
		return detailUrl_;
	}
	public String getSize() {
		return size_;
	}
	public String getAdded() {
		return added_;
	}

	public String getPeers() {
		return peers_;
	}
}
