package me.dje.Bubbles;

import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * The bubble class tracks the size, location and colour of a single bubble.
 */
public class Bubble {
	private float x, y, radius, maxRadius;
	private Paint paint;
	private BubbleWallpaper.ConfigAgent config;
	public boolean popped;
	
	/**
	 * Simple function for getting a random range.
	 *
	 * @param min The minimum int.
	 * @param max The maximum int.
	 * @return The random value.
	 */
	public static int randRange(int min, int max) {
		int mod = max - min;
		double val = Math.ceil(Math.random() * 1000000) % mod;
		return (int)val + min;
	}
	
	/**
	 * Create a bubble.
	 * 
	 * @param engine The Wallpaper engine
	 */
	Bubble(BubbleWallpaper.ConfigAgent config, int width, int height) {
		this.popped = false;
		
		paint = new Paint();
		paint.setColor(0xffffffff);
		/* paint.setARGB(randRange(0, 256),
				randRange(0, 256),
				randRange(0, 256),
				randRange(0, 256));
		*/
		paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);
		this.config = config;
		recycle(true, width, height);
	}
	
	/**
	 * Reinitialises the Bubble properties so that it appears to be a new 
	 * bubble.
	 * 
	 * Although a bit of elegance is sacrificed this seemed to result in a
	 * performance boost during initial testing.
	 * 
	 * @param initial
	 */
	public void recycle(boolean initial, int width, int height) {
		if(config.blur()) {
			this.paint.setMaskFilter(new BlurMaskFilter(this.radius/4 + 1, 
					BlurMaskFilter.Blur.NORMAL));
		} else {
			this.paint.setMaskFilter(null);
		}
		if(initial) {
			this.y = randRange(0, height);
		} else {
			// Start at the bottom if not initial
			this.y = height + (randRange(0, 21) - 10 ); 
		}
		this.x = randRange(0, width);
		this.radius = 1;
		this.maxRadius = randRange(3, config.bubbleSize());
		this.paint.setAlpha(randRange(100, 250));
		this.popped = false;
	}
	
	/**
	 * Update the size and position of a Bubble.
	 * 
	 * @param fps The current FPS
	 * @param angle The angle of the device
	 */
	public void update(int fps, float angle) {
		double speed = (config.speed() / config.getCurrentFPS()) * Math.log(this.radius);
		this.y -= speed;
		this.x += (randRange(0,3) - 1);
		
		if(this.radius < this.maxRadius) {
			this.radius += this.maxRadius / (((float)fps / config.speed()) * this.radius);
			if(this.radius > this.maxRadius) this.radius = this.maxRadius;
		}
		//this.x += (randRange(0,3) - 1) + (speed * (angle/90));
		//this.y -= (speed - (speed * ( (angle > 0 ? angle : -angle) / 90) ));
	}
	
	/**
	 * Test whether a bubble is no longer visible.
	 * 
	 * @param width Canvas width
	 * @param height Canvas height
	 * @return A boolean indicating that the Bubble has drifted off screen
	 */
	public boolean popped(int width, int height) {
		if(this.y + this.radius <= -20 || 
				this.y - this.radius >= height || 
				this.x + this.radius <= 0 || 
				this.x - this.radius >= width) {
			return true;
		}
		return false;
	}
	
	/**
	 * Unified method for drawing the bubble.
	 * 
	 * @param canvas The canvas to draw on
	 */
	public void draw(Canvas canvas) {
		canvas.drawCircle(x, y, radius, paint);
	}
	
}
