package me.dje.Bubbles;

import android.graphics.Paint;

/**
 * The bubble class tracks the size, location and colour of a single bubble.
 */
public class Bubble {
	private float x, y, radius, maxRadius;
	private Paint paint;
	private int maxSize = 10;
	private int baseFPS = 25;
	private BubbleWallpaper.BubbleEngine engine;
	public boolean popped;
	
	public static int randRange(int min, int max) {
		int mod = max - min;
		double val = Math.ceil(Math.random() * 1000000) % mod;
		return (int)val + min;
	}
	
	/**
	 * Create a bubble
	 * @param engine The Wallpaper engine
	 */
	Bubble(BubbleWallpaper.BubbleEngine engine, int size, int speed) {
		this.engine = engine;
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
		
		recycle(true, size, speed);
	}
	
	public void refresh() {
		this.recycle(false, this.maxSize, this.baseFPS);
	}
	
	/**
	 * 
	 * @param initial
	 */
	public void recycle(boolean initial, int size, int speed) {
		this.baseFPS = speed;
		this.maxSize = size;
		if(initial) {
			this.y = randRange(0, engine.getHeight());
		} else {
			// Start at the bottom if not initial
			this.y = engine.getHeight() + (randRange(0, 21) - 10 ); 
		}
		this.x = randRange(0, engine.getWidth());
		this.radius = 1;
		this.maxRadius = randRange(3, this.maxSize);
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
		// On fps 25 the speed is the radius
		float speed = this.radius / ((float)fps/this.baseFPS); // This is the speed at normal gravity
		//if(angle > 90) angle = 90;
		//else if(angle < -90) angle = -90;
		
		if(!popped) {
			if(this.radius < this.maxRadius) {
				this.radius += this.maxRadius / (((float)fps / this.baseFPS) * this.radius);
				//this.radius += (speed * ((float)this.height / 3));
				//this.radius += 0.1;
			}
			this.x += (randRange(0,3) - 1) + (speed * (angle/90));
			//if(angle < 0) this.y -= speed * (1 + angle/90);
			//else this.y -= speed - (speed * angle/90);
			this.y -= (speed - (speed * ( (angle > 0 ? angle : -angle) / 90) ));
			//this.y += speed * (speed / (angle/90));
			if(this.y + this.radius <= 0 || 
					this.y - this.radius >= engine.getHeight() || 
					this.x + this.radius <= 0 || 
					this.x - this.radius >= engine.getWidth()) {
				this.popped = true;
			}
		}
	}
	
	/* Boring accessors */
	
	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public float getX() {
		return x;
	}
	
	public float getY() {
		return y;
	}
	
	public float getRadius() {
		return radius;
	}
	
	public Paint getPaint() {
		return paint;
	}

	public void setRadius(float radius) {
		this.radius = radius;
	}
	
}
