package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FileBrowserActivity extends ListActivity {
	public final static String RESULT_SOURCE       = "filebrowser";
	public final static String RESULT_KEY_SOURCE   = "source";
	public final static String RESULT_KEY_FILEPATH = "filepath";
	
	private final static int FILE_TYPE_FILE   = 1;
	private final static int FILE_TYPE_FOLDER = 2;
	private final static int FILE_TYPE_PARENT = 3;
	
	private File currentDir_;									// currently opened folder
	private FileListAdapter adapter_;
	private Stack<File> dirStack_ = new Stack<File>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		currentDir_ = new File("/sdcard/");						// SD card's root directory
		showContent(currentDir_);
	}

	/** Shows the content of the given folder. */
	private void showContent(File f) {
		File[] dirs = f.listFiles();							// The given folder's files/folders
		this.setTitle(getString(R.string.filebrowser_current) + ": " + f.getName());
		List<FileListItem> dir = new ArrayList<FileListItem>();
		List<FileListItem> fls = new ArrayList<FileListItem>();
		try {
			for (File ff : dirs) {								// Files and folders to separate lists
				if (ff.isDirectory())
					dir.add(new FileListItem(ff.getName(), FILE_TYPE_FOLDER, ff.getAbsolutePath()));
				else {
					if (ff.getName().endsWith(".torrent"))
						fls.add(new FileListItem(ff.getName(), FILE_TYPE_FILE, ff.getAbsolutePath()));
				}
			}
		} catch (Exception e) {
			Log.v("FileChooserActivity", "Error during separating the list.");
		}
		Collections.sort(dir);									// List ordering
		Collections.sort(fls);
		dir.addAll(fls);										// Append file list to folder list
		
		if (!f.getName().equalsIgnoreCase("sdcard")) {							// If it's not the root directory:
			dir.add(0, new FileListItem("..", FILE_TYPE_PARENT, f.getParent()));		// parent directory to the head of the list
		}
		
		adapter_ = new FileListAdapter(FileBrowserActivity.this, R.layout.list_item_file, dir);
		this.setListAdapter(adapter_);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		FileListItem item = adapter_.getItem(position);
		if (item.getType()==FILE_TYPE_FOLDER) {							// folder
			dirStack_.push(currentDir_);
			currentDir_ = new File(item.getPath());
			showContent(currentDir_);
		} else if (item.getType()==FILE_TYPE_PARENT) {			// parent directory
			currentDir_ = dirStack_.pop();
			showContent(currentDir_);
		} else {																// file
			onFileClick(item);
		}
	}

	@Override
	public void onBackPressed() {
		if (dirStack_.size() == 0) {					// if root folder:
			Intent intent = new Intent();				// exit
			setResult(RESULT_CANCELED, intent);
			finish();
			return;
		}											// else: parent folder
		currentDir_ = dirStack_.pop();
		showContent(currentDir_);
	}

	/** Clicked on a file: returns the file path. */
	private void onFileClick(FileListItem item) {
		Intent intent = new Intent();
		intent.putExtra(RESULT_KEY_SOURCE, RESULT_SOURCE);
		intent.putExtra(RESULT_KEY_FILEPATH, item.getPath());
		setResult(RESULT_OK, intent);
		finish();
	}
	
	/** FileBrowser's Adapter. */
	class FileListAdapter extends ArrayAdapter<FileListItem> {
		private Context context_;
		private int id_;
		private List<FileListItem> items_;
		
		public FileListAdapter(Context context0, int textViewResourceId, List<FileListItem> objects) {
			super(context0, textViewResourceId, objects);
			context_ = context0;
			id_ = textViewResourceId;
			items_ = objects;
		}
		
		public FileListItem getItem(int i) {
			return items_.get(i);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
		     
			if (v == null) {
		    	LayoutInflater inflanter = (LayoutInflater)context_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		        v = inflanter.inflate(id_, null);
		    }
		     
		    final FileListItem item = items_.get(position);
		    if (item != null) {
		    	TextView tvFileName = (TextView) v.findViewById(R.filebrowser.filename);
		        TextView tvFileType = (TextView) v.findViewById(R.filebrowser.fileinfo);
		        ImageView image = (ImageView) v.findViewById(R.filebrowser.icon);
		             
		        if (tvFileName!=null)
		        	tvFileName.setText(item.getName());
		        if (tvFileType!=null) {
		        	if (item.getType()==FILE_TYPE_PARENT) {
		        		tvFileType.setText(getString(R.string.filebrowser_parent));
		        		image.setVisibility(ImageView.INVISIBLE);
		        	}
		        	else if (item.getType()==FILE_TYPE_FOLDER) {
		        		tvFileType.setText(getString(R.string.filebrowser_folder));
		        		image.setImageResource(R.drawable.icon_folder);
		        	}
		        	else {
		        		tvFileType.setText(getString(R.string.filebrowser_file));
		        		image.setImageResource(R.drawable.icon_file);
		        	}
		        }      
		    }
		    return v;
		}
	}
	
	/** Describes a file/folder. */
	class FileListItem implements Comparable<FileListItem> {
		private String name_;
		private int type_;
		private String path_;
		
		public FileListItem(String name, int type, String path) {
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
		
		public int compareTo(FileListItem o) {
			if(this.name_ != null)
				return this.name_.toLowerCase().compareTo(o.getName().toLowerCase()); 
			else 
				throw new IllegalArgumentException();
		}
	}

}