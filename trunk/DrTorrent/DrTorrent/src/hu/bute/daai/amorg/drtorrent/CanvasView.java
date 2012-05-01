package hu.bute.daai.amorg.drtorrent;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.view.View;

public class CanvasView extends View {

	public CanvasView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		
		Rect rect = new Rect();
		rect.set((int) (canvas.getWidth() * 0.1), (int) (canvas.getHeight() * 0.1), (int) (canvas.getWidth() * 0.9), (int) (canvas.getHeight() * 0.8));
		Paint paint = new Paint();
		paint.setColor(Color.GREEN);
		paint.setStyle(Style.FILL);
		
		canvas.drawRect(rect, paint);
		
	}

}
