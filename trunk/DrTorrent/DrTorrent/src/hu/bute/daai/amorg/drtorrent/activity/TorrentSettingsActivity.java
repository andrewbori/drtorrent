package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.FileSettingsListAdapter;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileListItem;
import hu.bute.daai.amorg.drtorrent.adapter.item.TorrentListItem;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;
import hu.bute.daai.amorg.drtorrent.torrentengine.File;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;

public class TorrentSettingsActivity extends Activity implements OnClickListener{

	private TorrentListItem torrent_ = null;
	private ArrayList<FileListItem> fileList_ = null;
	private ListView fileListView_ = null;
	private FileSettingsListAdapter<FileListItem> adapter_ = null;
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.torrent_settings_files);
		
		Intent intent = getIntent();
		if (intent != null) {
			Bundle bundle = intent.getExtras();
			if (bundle != null) {
				torrent_ = (TorrentListItem) bundle.getSerializable(TorrentService.MSG_KEY_TORRENT_ITEM);
				fileList_ = ((ArrayList<FileListItem>) bundle.getSerializable(TorrentService.MSG_KEY_FILE_LIST));
			}
		}
		
		((Button) findViewById(R.id.torrent_settings_btnOk)).setOnClickListener(this);
		((Button) findViewById(R.id.torrent_settings_btnCancel)).setOnClickListener(this);
		
		setTitle(torrent_.getName());
		
		fileListView_ = (ListView) findViewById(R.id.torrent_settings_lvFiles);
		adapter_ = new FileSettingsListAdapter<FileListItem>(this, fileList_);
		fileListView_.setAdapter(adapter_);
		
		((CheckBox) findViewById(R.id.torrent_settings_cbALl)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				for (int i = 0; i < fileList_.size(); i++) {
					if (isChecked) fileList_.get(i).setPriority(File.PRIORITY_NORMAL);
					else fileList_.get(i).setPriority(File.PRIORITY_SKIP);
					
					fileListView_.invalidateViews();
				}
			}
		});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.torrent_settings_btnOk:
				Intent intent = new Intent();
				intent.putExtra(TorrentService.MSG_KEY_FILE_LIST, fileList_);
				intent.putExtra(TorrentService.MSG_KEY_TORRENT_ITEM, torrent_);
				setResult(RESULT_OK, intent);
				finish();
				break;
				
			case R.id.torrent_settings_btnCancel:
				setResult(RESULT_CANCELED);
				finish();
				break;
			default:
		}
		
	}
	
}
