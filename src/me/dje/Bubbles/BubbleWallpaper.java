package me.dje.Bubbles;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.util.TimingLogger;
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
	private final String TAG = "BubbleWallpaper";

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
			long startMilli = SystemClock.uptimeMillis();
			final SurfaceHolder holder = getSurfaceHolder();
			Canvas surfaceCanvas = null;
			try {
				surfaceCanvas = holder.lockCanvas();
				if (surfaceCanvas != null) {
					int height = surfaceCanvas.getHeight();
					int width = surfaceCanvas.getWidth();
					
					if (collection == null || collection.length != config.bubbles()) {
						this.createBubbles(width, height);
					}
					
					/* Draw the background */
					surfaceCanvas.drawPaint(config.bgPaint());
					
					/* Draw each Bubble */
					for (int i = 0; i < collection.length; i++) {
						/* Move the Bubble */
						collection[i].update(config.fps(), sense.getRoll());
					
						/* Draw circle */
						collection[i].draw(surfaceCanvas);

						if (!sense.hold && collection[i].popped(width, height)) {
							collection[i].recycle(false, width, height);
						}
					}
					//surfaceCanvas.drawBitmap(bm, 0, 0, config.bgPaint());

				} else {
					Log.d(TAG, "Skipping frame");
				}
			} finally {
				// Unlock this in case of an error
				if (surfaceCanvas != null) {
					try {
						holder.unlockCanvasAndPost(surfaceCanvas);
						config.frame();
					} catch(IllegalArgumentException iae) {
						// Ignore this error, appart from catching it not sure how to handle it
						// I suspect this happens when the screen is rotated while the canvas is
						// locked
						Log.d(TAG, "Caught IllegalArgumentException doing unlockCanvasAndPost");
					}
				}
			}
			
			handler.removeCallbacks(drawBubbles);
			if (config.visible()) {
				long duration = SystemClock.uptimeMillis() - startMilli;
				handler.postDelayed(drawBubbles, (1000 / config.fps()) - duration);
			}
		}

		/**
		 * Create initial bubble instances in collection.
		 * 
		 * @param width Canvas width
		 * @param height Canvas height
		 */
		public void createBubbles(int width, int height) {
			this.collection = new Bubble[config.bubbles()];
			for (int i = 0; i < config.bubbles(); i++) {
				this.collection[i] = new Bubble(config, width, height);
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
				if (!sense.enabled())
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

	}

	class ConfigAgent implements
			SharedPreferences.OnSharedPreferenceChangeListener {
		private SensorAgent sensorAgent;

		private int prefFPS;
		private int prefBubbleSize;
		private int prefSpeed;
		private boolean pBlur;
		private boolean prefColorShift;
		private int prefBubbles;
		private boolean visible = true;
		private Paint pBGPaint;
		private SharedPreferences prefs;

		public ConfigAgent(SensorAgent sensorAgent) {
			this.pBGPaint = new Paint(DEFAULT_BLUE);
			prefColorShift = true;
			this.pBGPaint.setStyle(Paint.Style.FILL);
			this.sensorAgent = sensorAgent;
			prefs = getSharedPreferences(SHARED_PREFS_NAME, 0);
			prefs.registerOnSharedPreferenceChangeListener(this);
			this.onSharedPreferenceChanged(prefs, null);
		}

		public void onSharedPreferenceChanged(SharedPreferences sharedPrefs,
				String key) {

			if(key == null || key.compareTo("fps") == 0) {
				this.prefFPS = Integer.parseInt(sharedPrefs.getString("fps",
						"25"));
			}

			if(key == null || key.compareTo("enable_sensor") == 0) {
				if (sharedPrefs.getString("sensor", "Enable") == "Enable") {
					sensorAgent.enable();
				} else {
					sensorAgent.disable();
				}
			}
			if(key == null || key.compareTo("col_enter") == 0) {
				String col = sharedPrefs.getString("col_enter", "#112255");

				if (col.charAt(0) != '#')
					col = '#' + col;
				try {
					pBGPaint.setColor(Color.parseColor(col));
				} catch (Exception e) {
					pBGPaint.setColor(DEFAULT_BLUE);
				}
				pBGPaint.setAlpha(0xFF);
			} else if (key.compareTo("col_select") == 0) {
				SharedPreferences.Editor ed = sharedPrefs.edit();
				ed.putString("col_enter",
						sharedPrefs.getString("col_select", "#112255"));
				ed.commit();
			}

			if(key == null || key.compareTo("bubble_count") == 0) {
				int count = Integer.parseInt(sharedPrefs.getString(key, DEFAULT_BUBBLES));
				if (count != 0) {
					this.prefBubbles = count;
				}
			}
			if(key == null || key.compareTo("bubble_size") == 0) {
				try {
					this.prefBubbleSize = Integer.parseInt(sharedPrefs
							.getString("bubble_size", "10"));
				} catch (Exception e) {
					this.prefBubbleSize = 10;
				}
			}
			if(key == null || key.compareTo("col_shift") == 0) {
				try {
					this.prefColorShift = sharedPrefs.getBoolean("col_shift", false);
				} catch (Exception e) {
					this.prefColorShift = false;
				}
			}
			if(key == null || key.compareTo("fps") == 0) {
				try {
					this.prefFPS = Integer.parseInt(
							sharedPrefs.getString("fps", "25"));
					Log.d("BUBBLE", "FPS = " + this.prefFPS);
				} catch (Exception e) {
					this.prefFPS = 25;
				}
				this.avgFPS = 0;
			}
			if(key == null || key.compareTo("bubble_speed") == 0) {
				try {
					this.prefSpeed = Integer.parseInt(
							sharedPrefs.getString("bubble_speed", "25"));
				} catch (Exception e) {
					this.prefSpeed = 60;
				}
			}
			if(key == null || key.compareTo("blur") == 0) {
				this.pBlur = sharedPrefs.getBoolean("blur", false);
			}
		}
		
		public boolean blur() {
			return this.pBlur;
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
		
		public int speed() {
			return this.prefSpeed;
		}

		private int redDir = 0;
		private int greenDir = 0;
		private int blueDir = 0;
		private int redCnt = 0;
		private int greenCnt = 0;
		private int blueCnt = 0;
		private int frame = 0;

		/**
		 * Get the paint used for the background. For color shifting this 
		 * method also calculates the change.
		 * 
		 * @return current Paint for the background.
		 */
		public Paint bgPaint() {
			this.frame++;
			if (this.prefColorShift && this.frame >= (this.prefFPS / 20)) {
				this.frame = 0;
				
				int col = pBGPaint.getColor();
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
				pBGPaint.setColor(Color.argb(Color.alpha(col), red, green, blue));
			}
			return pBGPaint;
		}
		
		private long timingStart = 0, frameCount = 0;
		private double avgFPS = 0;
		
		/**
		 * Log a successful frame.
		 */
		public void frame() {
			if(timingStart == 0) {
				// The first one is free
				timingStart = SystemClock.uptimeMillis();
				return;
			}
			frameCount++;
			if(frameCount >= 2) {
				long currentTime = SystemClock.uptimeMillis();
				double elapsed = currentTime - timingStart;
				avgFPS = 1000 / (elapsed / frameCount); 
				timingStart = currentTime;
				frameCount = 0;
				//Log.d(TAG, "FPS: " + avgFPS);
				
			}
		}
		
		/**
		 * Get the current frame rate.
		 * 
		 * @return The current average frame rate.
		 */
		public double getCurrentFPS() {
			if(avgFPS < 10) {
				// Assume everything is ideal
				return (double)prefFPS;
			}
			return avgFPS;
		}
		
	}
	
	/**
	 * Dummy sensor agent. This class was originally part of the program but 
	 * needs to be redeveloped.
	 */
	class SensorAgent {
		private boolean hold = false;
		
		public int getRoll() {
			return 0;
		}
		
		public boolean enabled() {
			return false;
		}
		
		public void enable() {
			
		}
		
		public void disable() {
			
		}
	}
}