package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.FileBrowserAdapter;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileBrowserItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class FileBrowserActivity extends Activity {
	public final static String RESULT_SOURCE       = "filebrowser";
	public final static String RESULT_KEY_SOURCE   = "source";
	public final static String RESULT_KEY_FILEPATH = "filepath";
	public final static String KEY_PATH = "path";
	
	private File currentDir_;									// currently opened folder
	private FileBrowserAdapter adapter_;
	private TextView tvPath_;
	
	private ListView lwFolder_ = null;
	private ImageButton btnBack_ = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.file_browser);
		getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		
		currentDir_ = Environment.getExternalStorageDirectory();
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			String path = extras.getString(KEY_PATH);
			if (path != null) {
				currentDir_ = new File(path);
			}
		}
		
		tvPath_ = (TextView) findViewById(R.id.file_browser_tvPath);
		lwFolder_ = (ListView) findViewById(R.id.file_browser_listView);
		btnBack_ = (ImageButton) findViewById(R.id.file_browser_btnBack);
		((LinearLayout) findViewById(R.id.file_browser_layoutButtons)).setVisibility(LinearLayout.GONE);
		
		lwFolder_.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				FileBrowserItem item = adapter_.getItem(position);
				if (item.getType()==FileBrowserItem.FILE_TYPE_FOLDER) {			// folder
					currentDir_ = new File(item.getPath());
					showContent(currentDir_);
				} else if (item.getType()==FileBrowserItem.FILE_TYPE_PARENT) {	// parent directory
					if (currentDir_.getParentFile() != null) {
						currentDir_ = currentDir_.getParentFile();
						showContent(currentDir_);
					}
				} else {														// file
					onFileClick(item);
				}
			}
		});
		
		btnBack_.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (currentDir_.getParentFile() != null) {
					currentDir_ = currentDir_.getParentFile();
					showContent(currentDir_);
				}
			}
		});
		
		showContent(currentDir_);
	}

	/** Shows the content of the given folder. */
	private void showContent(File f) {
		File[] dirs = f.listFiles();							// The given folder's files/folders
		this.setTitle(f.getName());
		tvPath_.setText(f.getPath());
		List<FileBrowserItem> dir = new ArrayList<FileBrowserItem>();
		List<FileBrowserItem> fls = new ArrayList<FileBrowserItem>();
		try {
			for (File ff : dirs) {								// Files and folders to separate lists
				if (ff.isDirectory())
					dir.add(new FileBrowserItem(ff.getName(), FileBrowserItem.FILE_TYPE_FOLDER, ff.getAbsolutePath()));
				else {
					if (ff.getName().endsWith(".torrent"))
						fls.add(new FileBrowserItem(ff.getName(), FileBrowserItem.FILE_TYPE_FILE, ff.getAbsolutePath()));
				}
			}
		} catch (Exception e) {
			Log.v("FileBrowserActivity", "Error during separating the list.");
		}
		Collections.sort(dir);									// List ordering
		Collections.sort(fls);
		dir.addAll(fls);										// Append file list to folder list
		
		if (f.getParentFile() != null) {						// If it's not the root directory:
			dir.add(0, new FileBrowserItem("..", FileBrowserItem.FILE_TYPE_PARENT, f.getParent()));		// parent directory to the head of the list
		}
		
		adapter_ = new FileBrowserAdapter(FileBrowserActivity.this, R.layout.file_browser_item, dir);
		lwFolder_.setAdapter(adapter_);
	}

	@Override
	public void onBackPressed() {
		//if (currentDir_.getParentFile() == null) {	// if root folder:
		Intent intent = new Intent();					// exit
		setResult(RESULT_CANCELED, intent);
		finish();
		return;
		/*}													// else: parent folder
		currentDir_ = currentDir_.getParentFile();
		showContent(currentDir_);*/
	}

	/** Clicked on a file: returns the file path. */
	private void onFileClick(FileBrowserItem item) {
		Intent intent = new Intent();
		intent.putExtra(RESULT_KEY_SOURCE, RESULT_SOURCE);
		intent.putExtra(RESULT_KEY_FILEPATH, item.getPath());
		setResult(RESULT_OK, intent);
		finish();
	}
}