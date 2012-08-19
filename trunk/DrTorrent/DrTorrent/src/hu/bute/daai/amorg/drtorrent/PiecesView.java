package hu.bute.daai.amorg.drtorrent;

import hu.bute.daai.amorg.drtorrent.torrentengine.Bitfield;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.view.View;

public class PiecesView extends View {

	private Bitfield bitfield_ = null;
	private Bitfield downloadingBitfield_ = null;
	
	private boolean isMeasured_ = false;
	
	private int w_ = 0;
	private int h_ = 0;
	private double e_ = 0.0;
	
	public PiecesView(Context context) {
		super(context);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if (bitfield_ == null || downloadingBitfield_ == null) return;
		if (bitfield_.getLengthInBits() > downloadingBitfield_.getLengthInBits()) return;
		
		if (!isMeasured_) {
			double height = this.getHeight();
			double width = this.getWidth();
			
			double proportion = height / width;
			w_ = (int) Math.sqrt(((double) bitfield_.getLengthInBits()) / proportion);
			h_ = (int) (w_ * proportion);
			h_++;
			w_++;
			
			e_ = ((double) this.getWidth() / (double) w_);
			
			isMeasured_ = true;
		}
		
		int i = 0;
		double hd = 0.0;
		for (int hi = 0; hi < h_ && i < bitfield_.getLengthInBits(); hi++) {
			double wd = 0.0; 
			for (int wi = 0; wi < w_ && i < bitfield_.getLengthInBits(); wi++) {		
				Rect rectangle = new Rect();
				rectangle.set((int) (wd), (int) (hd), (int) (wd + e_), (int) (hd + e_));
				
				Paint paint = new Paint();
				if (bitfield_.isBitSet(i)) paint.setColor(Color.GREEN);
				else {
					if (downloadingBitfield_.isBitSet(i)) paint.setColor(Color.GRAY);
					else paint.setColor(Color.DKGRAY);
				}
				paint.setStyle(Style.FILL);
				
				canvas.drawRect(rectangle, paint);
				
				i++;
				wd += e_;
			}
			hd += e_;
		}
	}
	
	public void updateBitfield(Bitfield bitfield, Bitfield downloadingBitfield) {
		bitfield_ = bitfield;
		downloadingBitfield_ = downloadingBitfield;
		invalidate();
	}
}
