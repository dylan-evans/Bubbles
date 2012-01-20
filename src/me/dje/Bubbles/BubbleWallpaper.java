package me.dje.Bubbles;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/**
 * Bubble wall paper.
 * 
 * @author dylane
 *
 */
public class BubbleWallpaper extends WallpaperService {
	public static final String SHARED_PREFS_NAME = "bubble_settings";

	public Engine onCreateEngine() {		
		return new BubbleEngine();
	}
	
	/**
	 * 
	 * @author dylane
	 *
	 */
	class BubbleEngine extends Engine implements SensorEventListener, 
			SharedPreferences.OnSharedPreferenceChangeListener {
		private final int DEFAULT_BUBBLES = 128;
		private final int DEFAULT_BLUE = 0xff112255;
		//private final int DEFAULT_FANTA = 0xffdf8520;
		private final String TAG = "BubbleEngine";
		private boolean initial = true;
		private boolean visible = true;
		private boolean blue = true;
		
		public int width, height;
		private int fps = 25;
		private boolean useSensor = true;
		private boolean hold = false;
		private int bubbleSize = 10;
		
		private Bubble collection[];
		private Handler handler = new Handler();
		
		private int angle = 0;
		private float azimuth, pitch, roll;
		
		private final SensorManager sensorManager;
		private final Sensor sensor;
		
		private final Paint paint = new Paint();
		private final Paint bgPaint = new Paint();
		
		private SharedPreferences prefs;
		
		private final Runnable drawBubbles = new Runnable() {
			public void run() {
				drawFrame(false);
			}
		};
		
		
		/**
		 * Create and configure a new Bubble engine.
		 */	
		public BubbleEngine() {
			super();
			Log.v(TAG, "Creating Bubble engine");
			
			/* */
			sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
			sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			
			sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
			
			collection = new Bubble[DEFAULT_BUBBLES];
			paint.setColor(0xffffffff);
			paint.setStyle(Paint.Style.FILL);
			bgPaint.setColor(DEFAULT_BLUE);
			bgPaint.setStyle(Paint.Style.FILL);
			
			azimuth = pitch = roll = 0;
			
			prefs = getSharedPreferences(SHARED_PREFS_NAME, 0);
			prefs.registerOnSharedPreferenceChangeListener(this);
			this.onSharedPreferenceChanged(prefs, null);
		}
		
		/**
		 * Update all Bubble objects and draw the canvas.
		 * @param refresh Update dimensions
		 */
		public void drawFrame(boolean refresh) {
			//Log.v(TAG, "drawFrame");
			final SurfaceHolder holder = getSurfaceHolder();
			Canvas c = null; 
			
			try {
				c = holder.lockCanvas();
				if(c != null) {
					if(refresh) {
						this.height = c.getHeight();
						this.width = c.getWidth();
					}
					/* Draw the background */
					c.drawRect(new Rect(0, 0, c.getWidth(), c.getHeight()), bgPaint);

					/* Draw each Bubble */
					for(int i=0; i < collection.length; i++) {
						if(initial) {
							/* Create Bubble objects */
							collection[i] = new Bubble(this);
							collection[i].setMaxSize(bubbleSize);
							//bgPaint.setAlpha(0xFF);
						} else {
							/* Move the Bubble */
							collection[i].update(this.fps, this.roll);
						}
						/* Draw circle */
						c.drawCircle((int)collection[i].getX(), (int)collection[i].getY(), 
								(int)collection[i].getRadius(), collection[i].getPaint());
						if(!hold && collection[i].popped) {
							//collection[i] = new Bubble(c.getWidth(), c.getHeight());
							/* Less elegant but more efficient solution */
							collection[i].refresh(false);
						}
					}
					
					if(initial) initial = false;
				}
				
				
				//c.drawCircle(Bubble.randRange(1, c.getWidth()), 
						//Bubble.randRange(1, c.getHeight()), 
						//Bubble.randRange(1, 50), paint);
			} finally {
				if(c != null) holder.unlockCanvasAndPost(c);
			}
			
			handler.removeCallbacks(drawBubbles);
            if (visible) {
                handler.postDelayed(drawBubbles, 1000 / this.fps);
            }
		}
		
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);
			setTouchEventsEnabled(false);
		}
		
		@Override
        public void onVisibilityChanged(boolean visible) {
			this.visible = visible;
			sensorManager.unregisterListener(this);
            if (visible) {
            	if(useSensor)
            		sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
                drawFrame(true);
            } else {
                handler.removeCallbacks(drawBubbles);
                //sensorManager.unregisterListener(this);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            //drawFrame(true);
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }
        

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            handler.removeCallbacks(drawBubbles);
            sensorManager.unregisterListener(this);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                float xStep, float yStep, int xPixels, int yPixels) {
            //drawFrame(false);
        	
        }

		public void onAccuracyChanged(Sensor arg0, int arg1) {
			// TODO Auto-generated method stub
			
		}

		public void onSensorChanged(SensorEvent event) {
			this.azimuth = event.values[0];
			this.pitch = event.values[1];
			this.roll = event.values[2];
		}
		
		public void onTouchEvent(MotionEvent event) {
			if(event.getAction() == MotionEvent.ACTION_DOWN) {
				// Hold bubbles
				hold = true;
			}
			if(event.getAction() == MotionEvent.ACTION_UP) {
				// Release bubbles
				hold = false;
			}
		}

		public void onSharedPreferenceChanged(
				SharedPreferences sharedPrefs, String key) {
			//String col = sharedPreferences.getString("test", "blue");
			Log.d(TAG, "Preference key: '" + key + "'");
			
			if(key == null || key.compareTo("fps") == 0) {
				this.fps = Integer.parseInt(sharedPrefs.getString("fps", "25"));
				Log.d(TAG, "Setting FPS: " + this.fps);
			}
			
			if( key == null || key.compareTo("enable_sensor") == 0 ) {
				// Unregister as potential battery savior
				sensorManager.unregisterListener(this);
				if(sharedPrefs.getString("sensor", "Enable") == "Enable") {
					sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
					this.useSensor = true;
				} else {
					this.useSensor = false;	
				}
			}
			if( key == null || key.compareTo("col_enter") == 0) {
				String col = sharedPrefs.getString("col_enter", "#112255");
				
				if(col.charAt(0) != '#') col = '#' + col;
				try {
					bgPaint.setColor(Color.parseColor(col));
				} catch(Exception e) {
					bgPaint.setColor(DEFAULT_BLUE);
				}
				bgPaint.setAlpha(0xFF);
			} else if(key.compareTo("col_select") == 0) {
				SharedPreferences.Editor ed = sharedPrefs.edit();
				ed.putString("col_enter", sharedPrefs.getString("col_select", "#112255"));
				ed.commit();
			}
			
			if(key == null || key.compareTo("bubble_count") == 0) {
				int count = Integer.parseInt(sharedPrefs.getString(key,"0"));
				if(count != 0 && count != collection.length) {
					this.collection = new Bubble[count];
					this.initial = true;
				}
			}
			if(key == null || key.compareTo("bubble_size") == 0) {
				try {
					this.bubbleSize = Integer.parseInt(sharedPrefs.getString("bubble_size", "10"));
				} catch(Exception e) {
					this.bubbleSize = 10;
				}
				if(! this.initial) {
					// Update active bubbles
					for(int i = 0; i < collection.length; i++) {
						collection[i].setMaxSize(bubbleSize);
					}
				}
			}
			drawFrame(true);
		}
		
		public int getHeight() {
			return this.height;
		}
		
		public int getWidth() {
			return this.width;
		}
	}
	
}
