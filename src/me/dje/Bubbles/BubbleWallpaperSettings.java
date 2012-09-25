package me.dje.Bubbles;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;

public class BubbleWallpaperSettings extends PreferenceActivity 
		implements SharedPreferences.OnSharedPreferenceChangeListener {

	private PreferenceManager pm;
	
	public BubbleWallpaperSettings() {
		super();
		pm = this.getPreferenceManager();
		Log.d("BubbleWallpaperSettings", "Constructor");
	}
	
	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		if(pm == null) pm = this.getPreferenceManager();
		pm.setSharedPreferencesName(BubbleWallpaper.SHARED_PREFS_NAME);
		addPreferencesFromResource(R.xml.bubble_settings);
		pm.getSharedPreferences()
			.registerOnSharedPreferenceChangeListener(this);
	}
	
	public void onResume() {
        super.onResume();
    }
	
	public void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                this);
        super.onDestroy();
    }
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		getPreferenceManager().getSharedPreferences()
			.unregisterOnSharedPreferenceChangeListener(this);
	}
}
