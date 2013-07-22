package hu.bute.daai.amorg.drtorrent.activity;

import hu.bute.daai.amorg.drtorrent.Preferences;
import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedDictionary;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedList;
import hu.bute.daai.amorg.drtorrent.coding.bencode.BencodedString;
import hu.bute.daai.amorg.drtorrent.coding.sha1.SHA1;
import hu.bute.daai.amorg.drtorrent.file.FileComparator;
import hu.bute.daai.amorg.drtorrent.file.FileManager;
import hu.bute.daai.amorg.drtorrent.service.TorrentService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
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
	
	public final static String RESULT_KEY_TORRENT_PATH = "torrent";
	public final static String RESULT_KEY_DATA_PATH    = "data";
	
	private TextView tvFilePath_ = null;
	private EditText etTrackers_ = null;
	private CheckBox cbPrivateTorrent_ = null;
	private TextView tvFilePathSaveAs_ = null;
	private EditText etFileNameSaveAs_ = null;
	private CheckBox cbStartSeeding_ = null;
	
	private String filePath_ = null;
	private String filePathSaveAs_ = null;
	
	private ProgressDialog progress_ = null;
	
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
                    String privateTorrent = "0";
                    if (cbPrivateTorrent_.isChecked()) {
                        privateTorrent = "1";
                    }

                    String startSeeding = "0";
                    if (cbStartSeeding_.isChecked()) {
                        startSeeding = "1";
                    }

                    Preferences.setDefaultTrackers(trackers);

                    String fileSaveAs;
                    if (!filePathSaveAs_.equals("/")) {
                        fileSaveAs = filePathSaveAs_ + "/" + fileNameSaveAs;
                    } else {
                        fileSaveAs = filePathSaveAs_ + fileNameSaveAs;
                    }

                    TorrentCreatorTask torrentCreatorTask = new TorrentCreatorTask();
                    torrentCreatorTask.execute(filePath_, trackers, privateTorrent, fileSaveAs, startSeeding);
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
	
	private class TorrentCreatorTask extends AsyncTask<String, Integer, String> {
		
		private String filePath_ = null;
		private String filePathSaveAs_ = null;
		private boolean shouldStartSeeding_ = false;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			progress_ = new ProgressDialog(TorrentCreatorActivity.this);
			progress_.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress_.setProgress(0);
			progress_.setMessage(getString(R.string.creating_torrent));
			progress_.show();
		}
		
		@Override
		protected String doInBackground(String... params) {
			if (params == null || params.length < 5 || params[0] == null || params[1] == null || params[2] == null|| params[3] == null || params[4] == null) {
				return null;
			}
			
			filePath_ = params[0];
			String trackersStr = params[1];
			boolean isPrivate = params[2].equals("1") ? true : false;
			filePathSaveAs_ = params[3];
			shouldStartSeeding_ = params[4].equals("1") ? true : false;
			
			String announce = null;
			BencodedList announceList = new BencodedList();
			String[] trackers = trackersStr.split("\n");
			for (String tracker : trackers) {
				if (!tracker.equals("")) {
					BencodedList trackersSubList = new BencodedList();
					trackersSubList.append(new BencodedString(tracker));
					if (announce == null) {
						announce = tracker;
					}
					
					announceList.append(trackersSubList);
				}
			}
			
			
			File dirOrFile = new File(filePath_);
			name_ = dirOrFile.getName();
			
			if (dirOrFile.isDirectory()) {
				size_ = 0;
				processDir(dirOrFile, new BencodedList(), false);
			} else {
				size_ = dirOrFile.length();
			}
			
			long pieceLengthEstimator = size_ / 1024;	// No more pieces than 1024
			
			pieceLength_ = 16 * 1024;	// 16 kB
			while (pieceLengthEstimator > pieceLength_ && pieceLength_ < 16 * 1024 * 1024) {
				pieceLength_ *= 2;
			}
			
			if (dirOrFile.isDirectory()) {
				fileList_ = new BencodedList();
				baosPieces_ = new ByteArrayOutputStream();
				processDir(dirOrFile, new BencodedList(), true);
			} else {
				baosPieces_ = new ByteArrayOutputStream();
				calculateSHA1(dirOrFile);
				calculateSHA1Finished();
			}
			
			BencodedDictionary info = new BencodedDictionary();
			if (isPrivate) {
				info.addEntry("private", 1);
			}
			info.addEntry("name", name_);
			if (dirOrFile.isDirectory()) {
				info.addEntry("files", fileList_);
			} else {
				info.addEntry("length", size_);
			}
			
			info.addEntry("piece length", pieceLength_);
			info.addEntry("pieces", new BencodedString(baosPieces_.toByteArray()));
			
			BencodedDictionary torrent = new BencodedDictionary();
			if (announce != null) {
				torrent.addEntry("announce", announce);
			}
			if (announceList.count() > 1) {
				torrent.addEntry("announce-list", announceList);
			}
			//torrent.addEntry("comment", "Something");
			torrent.addEntry("created by", "DrTorrent " + TorrentService.APP_VERSION_NAME);
			torrent.addEntry("creation date", System.currentTimeMillis() / 1000L);
			torrent.addEntry("info", info);
			
			FileManager.removeFile(filePathSaveAs_);
			FileManager.write(filePathSaveAs_, 0, torrent.Bencode());
			
			return null;
		}
		
		private String name_;
		private BencodedList fileList_;
		private long size_;
		private int pieceLength_;
		private SHA1 sha1_ = new SHA1();
		private int sha1Length_ = 0;
		private ByteArrayOutputStream baosPieces_;
		
		private long processedFull_ = 0;
		/** Builds the Bencoded file list of the given directory. */
		private void processDir(File dir, BencodedList currentPath, boolean shouldBuild) {
			processDir(dir, currentPath, shouldBuild, true);
		}
		
		/** Builds the Bencoded file list of the given directory. */
		private void processDir(File dir, BencodedList currentPath, boolean shouldBuild, boolean isRootDir) {
			File[] files = dir.listFiles();
			Arrays.sort(files, new FileComparator());
			
			for (File file : files) {
				if (file.isFile()) {
					if (shouldBuild) {
						BencodedDictionary fileDict = new BencodedDictionary();
						fileDict.addEntry("length", file.length());
						
						BencodedList path = new BencodedList(currentPath);
						path.append(new BencodedString(file.getName()));
						fileDict.addEntry("path", path);
					
						fileList_.append(fileDict);
						
						calculateSHA1(file);
					} else {
						size_ += file.length();
					}
					
				
				} else if (file.isDirectory()) {
					BencodedList path = new BencodedList(currentPath);
					path.append(new BencodedString(file.getName()));
					processDir(file, path, shouldBuild, false);
				}
			}
			
			if (shouldBuild && isRootDir) {
				calculateSHA1Finished();
			}
		}
		
		/** Calculate the SHA1 checksums of the file. Puts the result to baosPieces_. */
		private void calculateSHA1(File file) {
			long length = file.length();
			for (long processedSize = 0; processedSize < length;) {
				long currentPos = processedSize;
				
				publishProgress((int)((processedFull_ * 100) / size_));
				
				int nextSize = pieceLength_ - sha1Length_;
				if (processedSize + nextSize > length) {
					nextSize = (int) (length - processedSize);
				}
				sha1Length_ += nextSize;
				
				processedSize += nextSize;
				processedFull_ += nextSize;

				FileManager.read(file.getPath(), currentPos, nextSize, sha1_);
				
				if (sha1Length_ == pieceLength_) {
					try {
						baosPieces_.write(SHA1.resultToByte(sha1_.digest()));
					} catch (IOException e) {
						e.printStackTrace();
					}
					sha1_.reset();
					sha1Length_ = 0;
				}
			}
		}
		
		/** Should call when finishing calculating to append the hash of the leftover data. */
		private void calculateSHA1Finished() {
			if (sha1Length_ > 0) {
				try {
					baosPieces_.write(SHA1.resultToByte(sha1_.digest()));
				} catch (IOException e) {
					e.printStackTrace();
				}
				sha1_.reset();
				sha1Length_ = 0;
			}
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			
			if (progress_ != null && values != null && values.length > 0) {
				progress_.setProgress(values[0]);
			}
		}
		
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			
			if (progress_ != null) {
				progress_.dismiss();
				progress_ = null;
			}
			Toast.makeText(TorrentCreatorActivity.this, R.string.creating_torrent_completed, Toast.LENGTH_SHORT).show();
			
			Intent intent = new Intent();
			if (shouldStartSeeding_) {
				File file = new File(filePath_);
				String dataPath = file.getParent();
				
				intent.putExtra(RESULT_KEY_TORRENT_PATH, filePathSaveAs_);				
				intent.putExtra(RESULT_KEY_DATA_PATH, dataPath);
				setResult(RESULT_OK, intent);
			} else {
				setResult(RESULT_CANCELED, intent);
			}
			finish();
		}
	}
	
	@Override
	protected void onDestroy() {
		if (progress_ != null) {
			progress_.dismiss();
			progress_ = null;
		}
		super.onDestroy();
	}
}
