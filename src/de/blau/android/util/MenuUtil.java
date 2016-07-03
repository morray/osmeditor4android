package de.blau.android.util;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ActionMenuView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import de.blau.android.Application;
import de.blau.android.R;

public class MenuUtil {
	
	static String DEBUG_TAG = MenuUtil.class.getName();
	
	static int MIN_WIDTH_DP = 64; // this is hardwired in ActionMenuView!!!
	
	private int maxItems = 0;
	private final int screenWidth;
	
	public MenuUtil(Context ctx) {
		// hardcoded calculation of how many icons we want to display
		//TODO de-hardcode
		DisplayMetrics metrics = Application.mainActivity.getResources().getDisplayMetrics();
		screenWidth = metrics.widthPixels;
		float widthDp = metrics.widthPixels / metrics.density;

		maxItems = (int) (widthDp/MIN_WIDTH_DP);
		Log.d(DEBUG_TAG,"pixel width " + metrics.widthPixels + " DP width " + widthDp + " maxItems " + maxItems);
	}

	public void reset() {
	}
	
	public void setShowAlways(Menu menu) {

		int nonVisibleItems = 0;
		for (int i=0;i < menu.size();i++) {
			MenuItem mi = menu.getItem(i);
			if (!mi.isVisible() || mi.getIcon() == null) {
				nonVisibleItems++;
			}
		}
		int tempMaxItems = maxItems;
		if ((menu.size() - nonVisibleItems) > maxItems) {
			// will have overflow menu
			tempMaxItems--;
		}
		
		// Log.d("MenuUtil","menu size " + menu.size() + " maxItems " + maxItems + " nonVisible " + nonVisibleItems);
		for (int i=0,j=0;i < menu.size();i++) { // max 10 even if we have more space
			MenuItem mi = menu.getItem(i);
			// Log.d("MenuUtil","menu " + mi.getTitle());
			if (j < Math.min(tempMaxItems,10)) {
				if (mi.isVisible() && mi.getIcon() != null) {
					MenuItemCompat.setShowAsAction(menu.getItem(i),MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
					j++;
				} 
			} else {
				MenuItemCompat.setShowAsAction(menu.getItem(i),MenuItemCompat.SHOW_AS_ACTION_NEVER);
			}
		}		
	}
	
	public static void setupBottomBar(Activity activity, ActionMenuView bar, boolean fullScreen, boolean light) {
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		if (Util.isLarge(activity)) {
			params.width = FrameLayout.LayoutParams.WRAP_CONTENT;
			params.gravity = Gravity.END;
		}
		bar.setLayoutParams(params);
		if (fullScreen) {
			if (light) {
				bar.setPopupTheme(R.style.Theme_noOverlapMenu_Light);
			} else {
				bar.setPopupTheme(R.style.Theme_noOverlapMenu);
			}
		}
	}
	
}
