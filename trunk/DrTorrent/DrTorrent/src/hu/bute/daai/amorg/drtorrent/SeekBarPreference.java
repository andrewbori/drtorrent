package hu.bute.daai.amorg.drtorrent;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {

	private static final String PREFERENCE_NS = "http://schemas.android.com/apk/src/hu.bute.daai.amorg.drtorrent.seekbarpreference";
	private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
	
	private final int defaultValue_;
	private final int entriesResource_;
	private final String[] entries_;
	private int selected_ = 0;
	
	private SeekBar seekBar_ = null;
	private TextView textView_ = null;
	
	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		setDialogLayoutResource(R.layout.dialog_seekbar_preference);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        
        setDialogIcon(null);
        
        defaultValue_ = attrs.getAttributeIntValue(ANDROID_NS, "defaultValue", 0);
        entriesResource_ = attrs.getAttributeResourceValue(PREFERENCE_NS, "entries", 0);
        
        entries_ = context.getResources().getStringArray(entriesResource_);
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
	    return a.getInteger(index, 0);
	}
	
	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		if (restorePersistedValue) {
			selected_ = getPersistedInt(defaultValue_);
		} else {
			selected_ = defaultValue_;
			persistInt(selected_);
		}
	}
	
	@Override
	protected void onBindDialogView(View view) {
		textView_ = (TextView) view.findViewById(R.id.dialog_seekbar_preference_text);
		textView_.setText(entries_[selected_]);
		seekBar_ = (SeekBar) view.findViewById(R.id.dialog_seekbar_preference_seekBar);
		seekBar_.setMax((entries_.length > 0) ? entries_.length - 1 : 0);
		seekBar_.setOnSeekBarChangeListener(this);
		seekBar_.setProgress(selected_);
		
		super.onBindDialogView(view);
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			if (shouldPersist()) {
				persistInt(selected_);
			}
			notifyChanged();
		}
	}
	
	@Override
	public CharSequence getSummary() {
		int value = getPersistedInt(defaultValue_);

		return entries_[value];
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (progress < entries_.length) {
			selected_ = progress;
			textView_.setText(entries_[progress]);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

}
