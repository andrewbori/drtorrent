package hu.bute.daai.amorg.drtorrent.adapter;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileBrowserItem;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/** FileBrowser's Adapter. */
public class FileBrowserAdapter extends ArrayAdapter<FileBrowserItem> {
	private Activity context_;
	private int id_;
	private List<FileBrowserItem> items_;
	
	public FileBrowserAdapter(Activity context, int textViewResourceId, List<FileBrowserItem> objects) {
		super(context, textViewResourceId, objects);
		context_ = context;
		id_ = textViewResourceId;
		items_ = objects;
	}
	
	public FileBrowserItem getItem(int i) {
		return items_.get(i);
	}
	
	@Override
	public View getView(int position, View reusableView, ViewGroup parent) {
		if (reusableView == null) {
	    	LayoutInflater inflanter = (LayoutInflater)context_.getLayoutInflater();
	    	reusableView = inflanter.inflate(id_, null);
	    }
	     
	    final FileBrowserItem item = items_.get(position);
	    if (item != null) {
	    	TextView tvFileName = (TextView) reusableView.findViewById(R.id.file_browser_item_filename);
	        ImageView image = (ImageView) reusableView.findViewById(R.id.file_browser_item_icon);
	             
	        if (tvFileName != null)
	        	tvFileName.setText(item.getName());
	        if (image != null) {
	        	if (item.getType() == FileBrowserItem.FILE_TYPE_PARENT) {
	        		image.setVisibility(ImageView.INVISIBLE);
	        	}
	        	else if (item.getType() == FileBrowserItem.FILE_TYPE_FOLDER) {
	        		image.setImageResource(R.drawable.icon_folder);
	        		image.setVisibility(ImageView.VISIBLE);
	        	}
	        	else {
	        		String filePath = item.getPath();
					File file = new File(filePath);
	                String extension = filePath.substring(filePath.lastIndexOf(".") + 1); // MimeTypeMap.getFileExtensionFromUrl(filePath);
	                if (extension.equalsIgnoreCase("torrent")) {
	                	image.setImageResource(R.drawable.icon_app);
		        		
	                } else {
		                String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		                
		                Intent intent = new Intent(Intent.ACTION_VIEW);
		                intent.setDataAndType(Uri.fromFile(file), type);
	
		        		final List<ResolveInfo> matches = context_.getPackageManager().queryIntentActivities(intent, 0);
		        		if (matches.size() > 0) {
		        		    final Drawable icon = matches.get(0).loadIcon(context_.getPackageManager());
		        		    image.setImageDrawable(icon);
		        		    
		        		} else {
		        			image.setImageResource(R.drawable.icon_file);
		        		}
	                }
	                image.setVisibility(ImageView.VISIBLE);
	        	}
	        }
	    }
	    return reusableView;
	}
}