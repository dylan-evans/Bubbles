package me.dje.Bubbles;

import java.lang.Thread;
import android.content.Context;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

public class Surface extends SurfaceView implements SurfaceHolder.Callback {

	public Surface(Context context) {
		super(context);
		
		Thread ok = new Thread() {
			public void Run() {
				
			}
		};
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}

}
