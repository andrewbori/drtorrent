package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.R;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class TorrentCreatorActivity extends Activity {
	
	private final static int RESULT_FILE_BROWSER_ACTIVITY 			= 1;
	private final static int RESULT_FOLDER_CHOOSER_ACTIVITY 		= 2;
	private final static int RESULT_FOLDER_CHOOSER_ACTIVITY_SAVE_AS = 3;
	
	public final static String RESULT_KEY_FILEPATH 			= "filepath";
	public final static String RESULT_KEY_TRACKERS 			= "trackers";
	public final static String RESULT_KEY_IS_PRIVATE 		= "private";
	public final static String RESULT_KEY_FILEPATH_SAVE_AS 	= "filepathSaveAs";
	public final static String RESULT_KEY_SHOULD_START		= "shouldStart";
	
	private TextView tvFilePath_ = null;
	private EditText etTrackers_ = null;
	private CheckBox cbPrivateTorrent_ = null;
	private TextView tvFilePathSaveAs_ = null;
	private EditText etFileNameSaveAs_ = null;
	private CheckBox cbStartSeeding_ = null;
	
	private String filePath_ = null;
	private String filePathSaveAs_ = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.torrent_creator);
		getWindow().setLayout(AbsListView.LayoutParams.FILL_PARENT, AbsListView.LayoutParams.FILL_PARENT);
		
		tvFilePath_ = (TextView) findViewById(R.id.torrent_creator_tvFilePath);
		tvFilePathSaveAs_ = (TextView) findViewById(R.id.torrent_creator_tvFilePathSaveAs);
		etTrackers_ = (EditText) findViewById(R.id.torrent_creator_etTrackers);
		etFileNameSaveAs_ = (EditText) findViewById(R.id.torrent_creator_etFileName);
		cbPrivateTorrent_ = (CheckBox) findViewById(R.id.torrent_creator_cbPrivateTorrent);
		cbStartSeeding_ = (CheckBox) findViewById(R.id.torrent_creator_cbStartSeeding);
		
		etTrackers_.setText(Preferences.getDefaultTrackers());
		filePathSaveAs_ = Preferences.getDownloadFolder();
		tvFilePathSaveAs_.setText(filePathSaveAs_);
		
		findViewById(R.id.torrent_creator_btnFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TorrentCreatorActivity.this, FileBrowserActivity.class);
                TorrentCreatorActivity.this.startActivityForResult(intent, RESULT_FILE_BROWSER_ACTIVITY);
            }
        });
		
		findViewById(R.id.torrent_creator_btnFolder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TorrentCreatorActivity.this, FolderChooserActivity.class);
                intent.putExtra(FolderChooserActivity.KEY_PATH, Environment.getExternalStorageDirectory().getPath());
                TorrentCreatorActivity.this.startActivityForResult(intent, RESULT_FOLDER_CHOOSER_ACTIVITY);
            }
        });
		
		findViewById(R.id.torrent_creator_btnFolderSaveAs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TorrentCreatorActivity.this, FolderChooserActivity.class);
                intent.putExtra(FolderChooserActivity.KEY_PATH, Environment.getExternalStorageDirectory().getPath());
                TorrentCreatorActivity.this.startActivityForResult(intent, RESULT_FOLDER_CHOOSER_ACTIVITY_SAVE_AS);
            }
        });
		
		findViewById(R.id.btnOk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileNameSaveAs = etFileNameSaveAs_.getText().toString();
                String trackers = etTrackers_.getText().toString();

                if (filePath_ != null && !filePath_.equals("") &&
                        filePathSaveAs_ != null && !filePathSaveAs_.equals("") &&
                        fileNameSaveAs != null && !fileNameSaveAs.equals("")) {

                    Preferences.setDefaultTrackers(trackers);

                    String fileSaveAs;
                    if (!filePathSaveAs_.equals("/")) {
                        fileSaveAs = filePathSaveAs_ + "/" + fileNameSaveAs;
                    } else {
                        fileSaveAs = filePathSaveAs_ + fileNameSaveAs;
                    }

    				Intent intent = new Intent();
    				intent.putExtra(RESULT_KEY_FILEPATH, filePath_);				
    				intent.putExtra(RESULT_KEY_TRACKERS, trackers);
    				intent.putExtra(RESULT_KEY_IS_PRIVATE, cbPrivateTorrent_.isChecked());
    				intent.putExtra(RESULT_KEY_FILEPATH_SAVE_AS, fileSaveAs);
    				intent.putExtra(RESULT_KEY_SHOULD_START, cbStartSeeding_.isChecked());
    				setResult(RESULT_OK, intent);
    				finish();
    				
                } else {
                    Toast.makeText(TorrentCreatorActivity.this, R.string.missing_parameters, Toast.LENGTH_SHORT).show();
                }
            }
        });
		
		findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				setResult(RESULT_CANCELED, intent);
				finish();
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case RESULT_FILE_BROWSER_ACTIVITY:
				if (resultCode == Activity.RESULT_OK) {
					filePath_ = data.getStringExtra(FileBrowserActivity.RESULT_KEY_FILEPATH);
					tvFilePath_.setText(filePath_);
					
					etFileNameSaveAs_.setText(new File(filePath_).getName() + ".torrent");
				}
				break;
		
			case RESULT_FOLDER_CHOOSER_ACTIVITY:
				if (resultCode == Activity.RESULT_OK) {
					filePath_ = data.getStringExtra(FolderChooserActivity.RESULT_KEY_PATH);
					tvFilePath_.setText(filePath_);
					
					etFileNameSaveAs_.setText(new File(filePath_).getName() + ".torrent");
				}
				break;
				
			case RESULT_FOLDER_CHOOSER_ACTIVITY_SAVE_AS:
				if (resultCode == Activity.RESULT_OK) {
					filePathSaveAs_ = data.getStringExtra(FolderChooserActivity.RESULT_KEY_PATH);
					tvFilePathSaveAs_.setText(filePathSaveAs_);
				}
				break;
		}
	}
}
