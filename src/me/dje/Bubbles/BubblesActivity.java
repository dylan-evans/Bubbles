package me.dje.Bubbles;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.view.SurfaceView;

public class BubblesActivity extends Activity {
	
	/**
	 * 
	 * @param savedInstanceState This is the instance state that was saved...
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SurfaceView sv = new SurfaceView(this);
        setContentView(sv);
    }
    
    
    public void onRestart() {
    	super.onRestart();
    }
    
    public void onStart() {
    	super.onStart();
    }
    
    public void onResume() {
    	super.onResume();
    }
    
    public void onPause() {
    	super.onPause();
    }
    
    public void onStop() {
    	super.onStop();
    }
    
    public void onDestroy() {
    	super.onDestroy();
    }
}
