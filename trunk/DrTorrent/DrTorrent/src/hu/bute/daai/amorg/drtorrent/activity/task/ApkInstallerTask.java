package hu.bute.daai.amorg.drtorrent.activity.task;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.network.HttpConnection;

import java.io.File;
import java.io.FileOutputStream;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

/** Task that downloads and installs the apk with the given url. */
public class ApkInstallerTask extends AsyncTask<String, Integer, Boolean> {
	
	Context context_ = null;
	ProgressDialog progressDialog_ = null;
	String path_ = null;
	
	public ApkInstallerTask(Context context) {
		context_ = context;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (progressDialog_ == null) {
			progressDialog_ = new ProgressDialog(context_);
		}
		progressDialog_.setMessage(context_.getString(R.string.downloading_the_app));
		progressDialog_.show();
	}
	
	@Override
	protected Boolean doInBackground(String... params) {
		if (params.length > 0) {
			final String url = params[0];
			final String filename = url.substring(url.lastIndexOf("/"));
			
			HttpConnection conn = new HttpConnection(url);
			
			byte[] content = conn.execute();
			if (content == null) {
				return false;
			}
			
			path_ = Environment.getExternalStorageDirectory().getPath().concat(filename);
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(path_);
				fos.write(content);
				fos.flush();
			} catch (Exception e) {
				return false;
			} finally {
				try {
					fos.close();
				} catch (Exception e) {
				}
			}
			
			return true;
		}
		return false;
	}
	
	 @Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		
		if (result && path_ != null) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
		    intent.setDataAndType(Uri.fromFile(new File(path_)), "application/vnd.android.package-archive");
		    context_.startActivity(intent);
		} else {
			Toast.makeText(context_, R.string.could_not_download_app, Toast.LENGTH_SHORT).show();
		}
		
		if (progressDialog_ != null) {
			progressDialog_.dismiss();
			progressDialog_ = null;
		}
		context_ = null;
	}
	 
	 @Override
	protected void onCancelled() {
		 if (progressDialog_ != null) {
			progressDialog_.dismiss();
			progressDialog_ = null;
		}
		context_ = null;
		super.onCancelled();
	}
}