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
		this.name_ = name;
		this.torrentUrl_ = torrentUrl;
		this.detailUrl_ = detailUrl;
		
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
		
		this.size_ = size;
		
		long dateLong = -1;
		try {
			dateLong = Long.parseLong(added);
		} catch (Exception e) {
		}
		Date date = new Date(dateLong);
		DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
		
		this.added_ = dateFormat.format(date);
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
