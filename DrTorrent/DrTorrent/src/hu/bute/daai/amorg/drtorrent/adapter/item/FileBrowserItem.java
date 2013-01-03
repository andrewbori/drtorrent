package hu.bute.daai.amorg.drtorrent.adapter.item;


/** Describes a file/folder. */
public class FileBrowserItem implements Comparable<FileBrowserItem> {
	public final static int FILE_TYPE_FILE   = 1;
	public final static int FILE_TYPE_FOLDER = 2;
	public final static int FILE_TYPE_PARENT = 3;
	
	final private String name_;
	final private int type_;
	final private String path_;
	
	public FileBrowserItem(final String name, final int type, final String path) {
		name_ = name;
		type_ = type;
		path_ = path;
	}
	
	public String getName() {
		return name_;
	}
	
	public int getType() {
		return type_;
	}
	
	public String getPath() {
		return path_;
	}
	
	public int compareTo(FileBrowserItem o) {
		if (this.name_ != null) {
			return this.name_.toLowerCase().compareTo(o.getName().toLowerCase());
		} else { 
			throw new IllegalArgumentException();
		}
	}
}