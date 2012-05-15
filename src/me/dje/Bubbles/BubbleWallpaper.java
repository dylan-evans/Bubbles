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
	private final int DEFAULT_BUBBLES = 128;
	private final int DEFAULT_BLUE = 0xff112255;
	private final String TAG = "BubbleEngine";
	
	
	public Engine onCreateEngine() {		
		return new BubbleEngine();
	}
	
	/**
	 * This is the the engine of the bubbles.
	 */
	class BubbleEngine extends Engine implements SensorEventListener, 
			SharedPreferences.OnSharedPreferenceChangeListener {
		private final Runnable drawBubbles;
		private Bubble collection[];
		private Handler handler = new Handler();
		private SensorEvent aEvent, mEvent;
		private boolean initial = true;
		private boolean visible = true;
		
		private int width, height, fps;
		private boolean hold, useSensor;
		private int bubbleSize = 10;
		
		private int redDir = 0;
		private int greenDir = 0;
		private int blueDir = 0;
		private int redCnt = 0;
		private int greenCnt = 0;
		private int blueCnt = 0;
		private int frame;
		
		private float azimuth, pitch, roll;
		
		private final Paint bgPaint = new Paint();
		
		private SharedPreferences prefs;
		private boolean bgColorShift = false;
		
		/**
		 * Create and configure a new Bubble engine.
		 */	
		public BubbleEngine() {
			super();
			
			enableSensor();
			
			// These are the defaults, which are mostly duplicated in xml settings
			collection = new Bubble[DEFAULT_BUBBLES];
			bgPaint.setColor(DEFAULT_BLUE);
			bgPaint.setStyle(Paint.Style.FILL);
			this.fps = 25;
			
			azimuth = pitch = roll = 0;
			aEvent = mEvent = null;
			
			prefs = getSharedPreferences(SHARED_PREFS_NAME, 0);
			prefs.registerOnSharedPreferenceChangeListener(this);
			this.onSharedPreferenceChanged(prefs, null);
			drawBubbles = new Runnable() {
				public void run() {
					drawFrame(false);
				}
			};
		}
		
		/**
		 * Update all Bubble objects and draw the canvas.
		 * @param refresh Update dimensions
		 */
		public void drawFrame(boolean refresh) {
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
					this.frame++;
					if(bgColorShift && this.frame == 5) {
						this.frame = 0;
						if(redCnt <= 0) {
							redDir = (int)Math.round(Math.random() * 100) % 3 - 1;
							redCnt = (int)Math.round(Math.random() * 1000) % 200;
						}
						if(greenCnt <= 0) {
							greenDir = (int)Math.round(Math.random() * 100) % 3 - 1;
							greenCnt = (int)Math.round(Math.random() * 1000) % 200;
						}
						if(blueCnt <= 0) {
							blueDir = (int)Math.round(Math.random() * 100) % 3 - 1;
							blueCnt = (int)Math.round(Math.random() * 1000) % 200;
						}
						int col = bgPaint.getColor();
						int red = Color.red(col);
						int green = Color.green(col);
						int blue = Color.blue(col);
						if((redDir > 0 &&  red < 255) || (redDir < 0 && red > 0))
							red += redDir;
						redCnt--;
						if((greenDir > 0 && green < 255) || (greenDir < 0 && green > 0))
							green += greenDir;
						greenCnt--;
						if((blueDir > 0 && blue < 255) || (blueDir < 0 && blue > 0))
							blue += blueDir;
						blueCnt--;
						bgPaint.setColor(Color.argb(Color.alpha(col), red, green, blue));
					}
					c.drawRect(new Rect(0, 0, c.getWidth(), c.getHeight()), bgPaint);

					/* Draw each Bubble */
					for(int i=0; i < collection.length; i++) {
						if(initial) {
							/* Create Bubble objects */
							collection[i] = new Bubble(this);
							collection[i].setMaxSize(bubbleSize);
						} else {
							/* Move the Bubble */
							collection[i].update(this.fps, this.roll);
						}
						/* Draw circle */
						c.drawCircle((int)collection[i].getX(), (int)collection[i].getY(), 
								(int)collection[i].getRadius(), collection[i].getPaint());
						if(!hold && collection[i].popped) {
							// collection[i] = new Bubble(c.getWidth(), c.getHeight());
							/* Less elegant but more efficient solution */
							collection[i].refresh(false);
						}
					}
					
					// Remove initial flag
					if(initial) initial = false;
				}
			} finally {
				// Unlock this in case of an error
				if(c != null) holder.unlockCanvasAndPost(c);
			}
			
			handler.removeCallbacks(drawBubbles);
            if (visible) {
                handler.postDelayed(drawBubbles, 1000 / this.fps);
            }
		}
		
		public void calculateAngle() {
			float R[] = new float[9];
			float I[] = new float[9];
			float gravity[] = new float[9];
			float geomagnetic[] = new float[9];
			SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
			
		}
		
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);
			setTouchEventsEnabled(false);
		}
		
		@Override
        public void onVisibilityChanged(boolean visible) {
			this.visible = visible;
			if (visible) {
            	if(useSensor)
            		enableSensor();
                drawFrame(true);
            } else {
                handler.removeCallbacks(drawBubbles);
                disableSensor();
            }
        }
		
		public void enableSensor() {
			SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
			Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
			Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
			Sensor magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
			sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_NORMAL);
			
		}
		
		public void disableSensor() {
			SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
			sensorManager.unregisterListener(this);
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
        	disableSensor();
        	super.onSurfaceDestroyed(holder);
            handler.removeCallbacks(drawBubbles);
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
			Log.d(TAG, "I got this event, yeah");
			if(this.hold || !this.visible) return; 
			if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				aEvent = event;
				Log.d(TAG, "I don't like to type a lot");
				
			} else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				mEvent = event;
				Log.d(TAG, "Got magnetic sensor");
			} else if(event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
				this.azimuth = event.values[0];
				this.pitch = event.values[1];
				this.roll = event.values[2];
				return;
			} else {
				return;
			}
			
			if(aEvent != null && mEvent != null) {
				float R[] = new float[16];
				float I[] = new float[16];
				SensorManager.getRotationMatrix(R, I, aEvent.values, mEvent.values);
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);
				this.azimuth = orientation[0];
				this.pitch = orientation[1];
				this.roll = orientation[2];
				Log.d(TAG, "Sense: " + this.azimuth + " " + this.pitch + " " + this.roll);
			}
			
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

		public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key) {
			SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
			Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			Log.d(TAG, "Preference key: '" + key + "'");
			
			if(key == null || key.compareTo("fps") == 0) {
				this.fps = Integer.parseInt(sharedPrefs.getString("fps", "25"));
				Log.d(TAG, "Setting FPS: " + this.fps);
			}
			
			if( key == null || key.compareTo("enable_sensor") == 0 ) {
				// Unregister as potential battery savior
				sensorManager.unregisterListener(this);
				if(sharedPrefs.getString("sensor", "Enable") == "Enable") {
					enableSensor();
					this.useSensor = true;
				} else {
					disableSensor();
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
			if(key == null || key.compareTo("col_shift") == 0) {
				try {
					this.bgColorShift = Boolean.parseBoolean(sharedPrefs.getString("col_shift", "false"));
				} catch(Exception e) {
					this.bgColorShift = false;
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
