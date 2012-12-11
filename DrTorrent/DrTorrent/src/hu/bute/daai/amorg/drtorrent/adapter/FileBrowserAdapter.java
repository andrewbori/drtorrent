package hu.bute.daai.amorg.drtorrent.adapter;

import hu.bute.daai.amorg.drtorrent.R;
import hu.bute.daai.amorg.drtorrent.adapter.item.FileBrowserItem;

import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
	        		image.setImageResource(R.drawable.icon_app);
	        		image.setVisibility(ImageView.VISIBLE);
	        	}
	        }
	    }
	    return reusableView;
	}
}