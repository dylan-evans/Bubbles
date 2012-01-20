package me.dje.Bubbles;

import android.graphics.Paint;

public class Bubble {
	private float x, y, radius, maxRadius;
	private Paint paint;
	private int width, height;
	private int maxSize = 10;
	public boolean popped;
	
	public static int randRange(int min, int max) {
		int mod = max - min;
		double val = Math.ceil(Math.random() * 1000000) % mod;
		return (int)val + min;
	}
	
	
	Bubble(int width, int height) {
		this.width = width;
		this.height = height;
		this.popped = false;
		
		paint = new Paint();
		paint.setColor(0xffffffff);
		paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);
		
		refresh(true);
	}
	
	public void refresh() {
		this.refresh(false);
	}
	
	public void refresh(boolean initial) {
		if(initial) {
			this.y = randRange(0, this.height);
		} else {
			// Start at the bottom if not initial
			this.y = this.height + (randRange(0, 21) - 10 ); 
		}
		this.x = randRange(0, this.width);
		this.radius = 1;
		this.maxRadius = randRange(3, this.maxSize);
		this.paint.setAlpha(randRange(100, 250));
		this.popped = false;
	}
	
	public void update(int fps, float angle) {
		// On fps 25 the speed is the radius
		float speed = this.radius / ((float)fps/25); // This is the speed at normal gravity
		//if(angle > 90) angle = 90;
		//else if(angle < -90) angle = -90;
		
		if(!popped) {
			if(this.radius < this.maxRadius) {
				this.radius += this.maxRadius / (((float)fps / 25) * this.radius);
				//this.radius += (speed * ((float)this.height / 3));
				//this.radius += 0.1;
			}
			this.x += (randRange(0,3) - 1) + (speed * (angle/90));
			//if(angle < 0) this.y -= speed * (1 + angle/90);
			//else this.y -= speed - (speed * angle/90);
			this.y -= (speed - (speed * ( (angle > 0 ? angle : -angle) / 90) ));
			//this.y += speed * (speed / (angle/90));
			if(this.y + this.radius <= 0 || 
					this.y - this.radius >= this.height || 
					this.x + this.radius <= 0 || 
					this.x - this.radius >= this.width) {
				this.popped = true;
			}
		}
	}
	
	/* Boring accessors */
	
	public void setDimension(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
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


	public int getMaxSize() {
		return maxSize;
	}


	public void setMaxSize(int maxBubbleSize) {
		this.maxSize = maxBubbleSize;
	}
	
}
