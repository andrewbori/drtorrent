package hu.bute.daai.amorg.drtorrent.adapter.item;

import hu.bute.daai.amorg.drtorrent.DrTorrentTools;
import hu.bute.daai.amorg.drtorrent.torrentengine.File;

import java.io.Serializable;

public class FileListItem implements Serializable {

	private static final long serialVersionUID = 1L;

	private String path_;
	private String name_;
	private String size_;
	private int priority_;
	
	public FileListItem(File file) {
		set(file);
	}
	
	public FileListItem(FileListItem item) {
		set(item);
	}
	
	public void set(File file) {
		path_ = file.getPath();
		name_ = file.getRelativePath();
		size_ = DrTorrentTools.bytesToString(file.getSize());
		priority_ = file.getPriority();
	}
	
	public void set(FileListItem item) {
		this.path_ = item.path_;
		this.name_ = item.name_;
		this.size_ = item.size_;
		this.priority_ = item.priority_;
	}
	
	public String getPath() {
		return path_;
	}
	
	public void setPath(String path) {
		this.path_ = path;
	}
	
	public String getName() {
		return name_;
	}
	
	public void setName(String name) {
		this.name_ = name;
	}
	
	public String getSize() {
		return size_;
	}
	
	public void setSize(String size) {
		this.size_ = size;
	}
	
	public int getPriority() {
		return priority_;
	}
	
	public void setPriority(int priotity) {
		this.priority_ = priotity;
	}
	
}
