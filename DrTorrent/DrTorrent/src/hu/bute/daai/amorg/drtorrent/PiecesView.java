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
	
	
	public PiecesView(Context context) {
		super(context);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if (bitfield_ == null) return;
		
		double height = canvas.getHeight() - 100.0;
		double width = canvas.getWidth();
		
		double proportion = height / width;
		int w = (int) Math.sqrt(((double) bitfield_.getLengthInBits()) / proportion);
		int h = (int) (w * proportion);
		h++;
		w++;
		
		double e;
		e = ((double) canvas.getWidth() / (double) w);
		// int he = (int) ((double) canvas.getHeight() / (double) h);
		
		int i = 0;
		for (int hi = 0; hi < h && i < bitfield_.getLengthInBits(); hi++) {
			for (int wi = 0; wi < w && i < bitfield_.getLengthInBits(); wi++) {		
				Rect rectangle = new Rect();
				//rectangle.set((int) (hi * he), (int) (wi * we), (int) (hi * he + he), (int) (wi * we + we));
				rectangle.set((int) (wi * e), (int) (hi * e), (int) (wi * e + e), (int) (hi * e + e));
				
				Paint paint = new Paint();
				if (bitfield_.isBitSet(i)) paint.setColor(Color.GREEN);
				else paint.setColor(Color.DKGRAY);
				paint.setStyle(Style.FILL);
				
				canvas.drawRect(rectangle, paint);
				
				i++;
			}
		}
	}

	public void updateBitfield(Bitfield bitfield) {
		bitfield_ = bitfield;
		invalidate();
	}

}
