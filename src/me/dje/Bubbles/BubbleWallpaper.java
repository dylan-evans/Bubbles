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
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/**
 * Bubble wall paper.
 * 
 * @author Dylan Evans
 * 
 */
public class BubbleWallpaper extends WallpaperService {
	public static final String SHARED_PREFS_NAME = "bubble_settings";
	private final String DEFAULT_BUBBLES = "128";
	private final int DEFAULT_BLUE = 0xff112255;
	//private final String TAG = "BubbleEngine";

	/**
	 * Create a new instance of the BubbleEngine
	 */
	public Engine onCreateEngine() {
		return new BubbleEngine(this);
	}

	/**
	 * This is the the engine of the bubbles.
	 */
	class BubbleEngine extends Engine {
		private final Runnable drawBubbles;
		private Bubble collection[];
		private Handler handler = new Handler();

		private int width, height;

		private SensorAgent sense;
		private ConfigAgent config;

		/**
		 * Create and configure a new Bubble engine.
		 */
		public BubbleEngine(Context context) {
			super();
			
			this.collection = null;
			
			sense = new SensorAgent();
			config = new ConfigAgent(sense);

			drawBubbles = new Runnable() {
				public void run() {
					drawFrame(false);
				}
			};

		}

		/**
		 * Update all Bubble objects and draw the canvas.
		 * 
		 * @param refresh
		 *            Update dimensions
		 */
		public void drawFrame(boolean refresh) {
			final SurfaceHolder holder = getSurfaceHolder();
			Canvas c = null;

			try {
				c = holder.lockCanvas();
				if (c != null) {
					this.height = c.getHeight();
					this.width = c.getWidth();
					
					if (collection == null || collection.length != config.bubbles()) {
						this.createBubbles();
					}
					
					/* Draw the background */
					c.drawRect(new Rect(0, 0, this.width, this.height),
							config.bgPaint());

					/* Draw each Bubble */
					for (int i = 0; i < collection.length; i++) {
						/* Move the Bubble */
						collection[i].update(config.fps(), sense.getRoll());

						/* Draw circle */
						c.drawCircle((int) collection[i].getX(),
								(int) collection[i].getY(),
								(int) collection[i].getRadius(),
								collection[i].getPaint());

						if (!sense.hold && collection[i].popped) {
							// collection[i] = new Bubble(c.getWidth(),
							// c.getHeight());
							/* Less elegant but more efficient solution */
							collection[i].recycle(false);
						}
					}

				}
			} finally {
				// Unlock this in case of an error
				if (c != null)
					holder.unlockCanvasAndPost(c);
			}

			handler.removeCallbacks(drawBubbles);
			if (config.visible()) {
				handler.postDelayed(drawBubbles, 1000 / config.fps());
			}
		}

		public void createBubbles() {
			int size = config.bubbleSize();
			this.collection = new Bubble[config.bubbles()];
			for (int i = 0; i < config.bubbles(); i++) {
				this.collection[i] = new Bubble(this);
				this.collection[i].setMaxSize(size);
			}
		}

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);
			setTouchEventsEnabled(false);
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			if (visible) {
				config.show();
				if (sense.enabled())
					sense.enable();
				drawFrame(true);
			} else {
				config.hide();
				handler.removeCallbacks(drawBubbles);
				sense.disable();
			}
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {
			super.onSurfaceCreated(holder);
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			sense.disable();
			super.onSurfaceDestroyed(holder);
			handler.removeCallbacks(drawBubbles);
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset, float xStep,
				float yStep, int xPixels, int yPixels) {
		}

		public int getHeight() {
			return this.height;
		}

		public int getWidth() {
			return this.width;
		}
	}

	class ConfigAgent implements
			SharedPreferences.OnSharedPreferenceChangeListener {
		private SensorAgent sensorAgent;

		private int prefFPS;
		private int prefBubbleSize;
		private boolean prefColorShift;
		private int prefBubbles;
		private boolean visible = true;
		private Paint prefPaint;
		private SharedPreferences prefs;

		public ConfigAgent(SensorAgent sensorAgent) {
			this.prefPaint = new Paint(DEFAULT_BLUE);
			prefColorShift = true;
			this.prefPaint.setStyle(Paint.Style.FILL);
			this.sensorAgent = sensorAgent;
			prefs = getSharedPreferences(SHARED_PREFS_NAME, 0);
			prefs.registerOnSharedPreferenceChangeListener(this);
			this.onSharedPreferenceChanged(prefs, null);
		}

		public void onSharedPreferenceChanged(SharedPreferences sharedPrefs,
				String key) {

			if (key == null || key.compareTo("fps") == 0) {
				this.prefFPS = Integer.parseInt(sharedPrefs.getString("fps",
						"25"));
			}

			if (key == null || key.compareTo("enable_sensor") == 0) {
				if (sharedPrefs.getString("sensor", "Enable") == "Enable") {
					sensorAgent.enable();
				} else {
					sensorAgent.disable();
				}
			}
			if (key == null || key.compareTo("col_enter") == 0) {
				String col = sharedPrefs.getString("col_enter", "#112255");

				if (col.charAt(0) != '#')
					col = '#' + col;
				try {
					prefPaint.setColor(Color.parseColor(col));
				} catch (Exception e) {
					prefPaint.setColor(DEFAULT_BLUE);
				}
				prefPaint.setAlpha(0xFF);
			} else if (key.compareTo("col_select") == 0) {
				SharedPreferences.Editor ed = sharedPrefs.edit();
				ed.putString("col_enter",
						sharedPrefs.getString("col_select", "#112255"));
				ed.commit();
			}

			if (key == null || key.compareTo("bubble_count") == 0) {
				int count = Integer.parseInt(sharedPrefs.getString(key, DEFAULT_BUBBLES));
				if (count != 0) {
					this.prefBubbles = count;
				}
			}
			if (key == null || key.compareTo("bubble_size") == 0) {
				try {
					this.prefBubbleSize = Integer.parseInt(sharedPrefs
							.getString("bubble_size", "10"));
				} catch (Exception e) {
					this.prefBubbleSize = 10;
				}
			}
			if (key == null || key.compareTo("col_shift") == 0) {
				try {
					this.prefColorShift = sharedPrefs.getBoolean("col_shift", false);
				} catch (Exception e) {
					this.prefColorShift = false;
				}
			}
		}

		public int bubbles() {
			return this.prefBubbles;
		}

		public int fps() {
			return prefFPS;
		}

		public int bubbleSize() {
			return prefBubbleSize;
		}

		public boolean colorShift() {
			return this.prefColorShift;
		}
		
		public void show() {
			this.visible = true;
		}
		
		public void hide() {
			this.visible = false;
		}
		
		public boolean visible() {
			return visible;
		}

		private int redDir = 0;
		private int greenDir = 0;
		private int blueDir = 0;
		private int redCnt = 0;
		private int greenCnt = 0;
		private int blueCnt = 0;
		private int frame = 0;

		public Paint bgPaint() {
			this.frame++;
			if (this.prefColorShift && this.frame >= (this.prefFPS / 20)) {
				this.frame = 0;
				
				int col = prefPaint.getColor();
				int red = Color.red(col);
				int green = Color.green(col);
				int blue = Color.blue(col);
				
				// avg is used to keep the screen away from pure black and white
				int avg = (red + green + blue) / 3;
				int forceDir = 0;
				if(avg > 0xD0) {
					forceDir = -1;
				} else if(avg < 0x20) {
					forceDir = 1;
				}
				
				if ((forceDir < 0 && red > 0xD0 )||( forceDir > 0 && red < 0x20)) {
					redDir += forceDir;
					redCnt = 0;
				} else if(redCnt <= 0) {
					redDir = (int) Math.round(Math.random() * 100) % 3 - 1;
					redCnt = (int) Math.round(Math.random() * 1000) % 200;
				}
				if ((forceDir < 0 && green > 0xD0 )||( forceDir > 0 && green < 0x20)) {
					greenDir += forceDir;
					greenCnt = 0;
				} else if(greenCnt <= 0) {
					greenDir = (int) Math.round(Math.random() * 100) % 3 - 1;
					greenCnt = (int) Math.round(Math.random() * 1000) % 200;
				}
				if ((forceDir < 0 && blue > 0xD0 )||( forceDir > 0 && blue < 0x20)) {
					blueDir += forceDir;
					blueCnt = 0;
				} else if(blueCnt <= 0) {
					blueDir = (int) Math.round(Math.random() * 100) % 3 - 1;
					blueCnt = (int) Math.round(Math.random() * 1000) % 200;
				}
				
				if ((redDir > 0 && red < 255) || (redDir < 0 && red > 0))
					red += redDir;
				redCnt--;
				if ((greenDir > 0 && green < 255)
						|| (greenDir < 0 && green > 0))
					green += greenDir;
				greenCnt--;
				if ((blueDir > 0 && blue < 255) || (blueDir < 0 && blue > 0))
					blue += blueDir;
				blueCnt--;
				prefPaint.setColor(Color.argb(Color.alpha(col), red, green, blue));
			}
			return prefPaint;
		}
	}

	class SensorAgent implements SensorEventListener {
		private boolean enabled;
		private boolean hold;
		//private int azimuth, pitch;
		private int roll;

		public SensorAgent() {
			enabled = false;
			hold = false;
			
		}

		public void enable() {
			SensorManager sensorManager = (SensorManager) 
					getSystemService(Context.SENSOR_SERVICE);
			Sensor sensor = sensorManager
					.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			sensorManager.registerListener(this, sensor,
					SensorManager.SENSOR_DELAY_NORMAL);
			Sensor accelerometer = sensorManager
					.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			sensorManager.registerListener(this, accelerometer,
					SensorManager.SENSOR_DELAY_NORMAL);
			Sensor magnetic = sensorManager
					.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
			sensorManager.registerListener(this, magnetic,
					SensorManager.SENSOR_DELAY_NORMAL);
			this.enabled = true;
		}

		public void disable() {
			SensorManager sensorManager = (SensorManager)
					getSystemService(Context.SENSOR_SERVICE);
			sensorManager.unregisterListener(this);
			this.enabled = false;
		}
		
		public boolean enabled() {
			return this.enabled;
		}

		public void onSensorChanged(SensorEvent event) {
			SensorEvent aEvent = null, mEvent = null;
			if (!this.enabled)
				return;
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				aEvent = event;
			} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				mEvent = event;
			} else if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
				// this.azimuth = event.values[0];
				// this.pitch = event.values[1];
				// this.roll = event.values[2];
				return;
			} else {
				return;
			}

			if (aEvent != null && mEvent != null) {
				float R[] = new float[16];
				float I[] = new float[16];
				SensorManager.getRotationMatrix(R, I, aEvent.values,
						mEvent.values);
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);
				// this.azimuth = orientation[0];
				// this.pitch = orientation[1];
				// this.roll = orientation[2];
			}

			// this.azimuth = event.values[0];
			// this.pitch = event.values[1];
			// this.roll = event.values[2];
		}

		public void onTouchEvent(MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				// Hold bubbles
				hold = true;
			}
			if (event.getAction() == MotionEvent.ACTION_UP) {
				// Release bubbles
				hold = false;
			}
		}

		public void onAccuracyChanged(Sensor arg0, int arg1) {
			
		}
		
		public int getRoll() {
			return roll;
		}
	}
}