package hu.bute.daai.amorg.drtorrent.adapter.item;

import hu.bute.daai.amorg.drtorrent.Quantity;
import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.torrentengine.File;

import java.io.Serializable;

import android.content.Context;

public class FileListItem implements Serializable {

	private static final long serialVersionUID = 1L;

	private int index_ = -1;
	private String path_;
	private String name_;
	private Quantity downloadedSize_;
	private Quantity size_;
	private int priority_;
	private boolean isComplete_;
	
	public FileListItem(File file) {
		set(file);
	}
	
	public FileListItem(FileListItem item) {
		set(item);
	}
	
	public void set(File file) {
		index_ = file.index();
		path_ = file.getFullPath();
		name_ = file.getRelativePath();
		downloadedSize_ = new Quantity(file.getDownloadedSize(), Quantity.SIZE);
		size_ = new Quantity(file.getSize(), Quantity.SIZE);
		priority_ = file.getPriority();
		isComplete_ = file.isComplete();
	}
	
	public void set(FileListItem item) {
		this.index_ = item.index_;
		this.path_ = item.path_;
		this.name_ = item.name_;
		this.downloadedSize_ = item.downloadedSize_;
		this.size_ = item.size_;
		this.priority_ = item.priority_;
		this.isComplete_ = item.isComplete_;
	}
	
	public int getIndex() {
		return index_;
	}
	
	public String getPath() {
		return path_;
	}
	
	public String getName() {
		return name_;
	}
	
	public String getSize(Context context) {
		return size_.toString(context);
	}
	
	public String getDownloadedAndFullSize(Context context) {
		return downloadedSize_.toString(context, size_.getUnit()) + "/" + size_.toString(context);
	}
	
	public int getPriority() {
		return priority_;
	}
	
	public String getPriorityString(Context context) {
		switch (priority_) {
			case File.PRIORITY_HIGH:
				return context.getString(R.string.priority_high);
			case File.PRIORITY_NORMAL:
				return context.getString(R.string.priority_normal);
			case File.PRIORITY_LOW:
				return context.getString(R.string.priority_low);
			default:
				return context.getString(R.string.priority_skip);
		}
	}
	
	public void setPriority(int priotity) {
		this.priority_ = priotity;
	}
	
	public boolean isComplete() {
		return isComplete_;
	}
	
	@Override
	public boolean equals(Object another) {
		return this.index_ == ((FileListItem) another).index_;
	}
	
}
