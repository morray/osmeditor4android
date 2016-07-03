package de.blau.android;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.BugFixedAppCompatActivity;


/**
 * Minimal system for viewing help files
 * Currently only html format is supported directly
 * @author simon
 *
 */
public class HelpViewer extends BugFixedAppCompatActivity {
	
	static String DEBUG_TAG = HelpViewer.class.getName();
	
	class HelpItem implements Comparable<HelpItem> {
		boolean displayLanguage = false;
		String language;
		int order;
		String topic;
		
		@Override
		public int compareTo(HelpItem another) {
			if (order < Integer.MAX_VALUE) {
				if (order > another.order) {
					return 1;
				} else if (order < another.order) {
					return -1;
				}
			}
			return topic.compareTo(another.topic); // sort the rest alphabetically
		}
		
		@Override
		public String toString() {
			return topic + (displayLanguage ? " (" + language + ")": "");
		}
	}
	
	
	public static final String TOPIC = "topic";
	WebView helpView;
	
	ActionBarDrawerToggle mDrawerToggle;
	// drawer that will be our ToC
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	ArrayAdapter<HelpItem> tocAdapter;

	public static void start(@NonNull Context context, @StringRes int topic) {
		Intent intent = new Intent(context, HelpViewer.class);
		intent.putExtra(TOPIC, topic);
		context.startActivity(intent);
	}

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Preferences prefs = new Preferences(this);
		if (prefs.lightThemeEnabled()) {
			setTheme(R.style.Theme_customHelpViewer_Light);
		}
		
		super.onCreate(savedInstanceState);
		int topicId = (Integer)getIntent().getSerializableExtra(TOPIC);
		String topic = getString(topicId); // this assumes that the resources are the same, which is probably safe
		
		setContentView(R.layout.help_drawer);

		
//        // Find the toolbar view inside the activity layout
//        Toolbar toolbar = (Toolbar) findViewById(R.id.helpToolbar);
//        // Sets the Toolbar to act as the ActionBar for this Activity window.
//        // Make sure the toolbar exists in the activity and is not null
//        setSupportActionBar(toolbar);

		ActionBar actionbar = getSupportActionBar();
		if (actionbar == null) {
			Log.d("HelpViewer", "No actionbar"); // fail?
			return;
		}
		actionbar.setDisplayShowHomeEnabled(true);
		actionbar.setTitle(getString(R.string.menu_help) + ": " + topic);
		actionbar.setDisplayShowTitleEnabled(true);
		actionbar.show();


		
		
		
		// add our content
		FrameLayout fl =  (FrameLayout) findViewById(R.id.content_frame);
		helpView = new WebView(this);
		WebSettings helpSettings = helpView.getSettings();
		helpSettings.setDefaultFontSize(12);
		helpSettings.setSupportZoom(true);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			helpSettings.setDisplayZoomControls(false); // don't display +-
		} else {
			helpSettings.setBuiltInZoomControls(true);
		}
		helpView.setWebViewClient(new HelpViewWebViewClient());
		fl.addView(helpView);
		
		// set up the drawer
		mDrawerLayout = (DrawerLayout) findViewById(R.id.help_drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.help_left_drawer);
		
		actionbar.setHomeButtonEnabled(true);
		actionbar.setDisplayHomeAsUpEnabled(true);
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.okay, R.string.okay);
		mDrawerToggle.setDrawerIndicatorEnabled(true);
		mDrawerLayout.addDrawerListener(mDrawerToggle);	

		try {
			List<String> defaultList = Arrays.asList(getResources().getAssets().list("help/" + Locale.getDefault().getLanguage()));
			List<String> enList = Arrays.asList(getResources().getAssets().list("help/en"));
			String defaultLanguage = Locale.getDefault().getLanguage();
			
			TypedArray tocRes = getResources().obtainTypedArray(R.array.help_tableofcontents);
			
			HashMap <String,HelpItem> tocList = new HashMap<String,HelpItem>();
					
			for (int i=0;i<tocRes.length();i++) {
				String tocTopic = tocRes.getString(i);
				// Log.d("HelpViewer", "TOC " + tocTopic); 
				if (defaultList.contains(tocTopic + ".html")) {
					// Log.d("HelpViewer", "TOC " + locale + " " + tocTopic); 
					HelpItem h = new HelpItem();
					h.language = defaultLanguage;
					h.topic = tocTopic;
					h.order = i;	
					if (!tocList.containsKey(h.topic)) {
						tocList.put(h.topic,h);
					}
				} else if (enList.contains(tocTopic + ".html")){
					// Log.d("HelpViewer", "TOC en " + tocTopic);
					HelpItem h = new HelpItem();
					h.language = "en";
					h.displayLanguage = true;
					h.topic = tocTopic;
					h.order = i;	
					if (!tocList.containsKey(h.topic)) {
						tocList.put(h.topic,h);
					}
				}
			}
			tocRes.recycle();
			
			List<HelpItem> items = new ArrayList<HelpItem>(tocList.values());
			Collections.sort(items);
			HelpItem[] toc = new HelpItem[items.size()];
			items.toArray(toc);
			
			tocAdapter = new ArrayAdapter<HelpItem>(this, R.layout.help_drawer_item,R.id.help_drawer_item, toc);
			
			mDrawerList.setAdapter(tocAdapter);
			mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

			String helpFile = "help/" + Locale.getDefault().getLanguage() + "/"  + topic + ".html";
			Log.d("HelpViewer","1 Looking for help file: " + helpFile);
			if (!defaultList.contains(topic + ".html")) {
				helpFile = "help/en/"  + topic + ".html";
				if (!enList.contains(topic + ".html")) {
					helpFile = "help/en/no_help.html";
					mDrawerLayout.openDrawer(mDrawerList);
				}
			}
			helpView.loadUrl("file:///android_asset/" + helpFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates the menu from the XML file "main_menu.xml".<br> {@inheritDoc}
	 */
 	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater(); 
		inflater.inflate(R.menu.help_menu, menu);
		return true;
 	}
 	
 	@Override
 	public boolean onOptionsItemSelected(final MenuItem item) {
 		if (mDrawerToggle.onOptionsItemSelected(item)) {
 			return true;
 		}
 		Log.d(DEBUG_TAG, "onOptionsItemSelected");
 		switch (item.getItemId()) {
 		case R.id.help_menu_back:
 			if (helpView.canGoBack()) {
 				helpView.goBack();
 				// getSupportActionBar().setTitle(getString(R.string.menu_help) + ": " + getTopic(helpView.getUrl()));
 			} else {
 				onBackPressed(); // return to caller
 			}
 			return true;

 		case R.id.help_menu_forward:
 			if (helpView.canGoForward()) {
 				helpView.goForward();
 				// getSupportActionBar().setTitle(getString(R.string.menu_help) + ": " + getTopic(helpView.getUrl()));
 			}
 			return true;
 		}
 		return false;
 	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}
	
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			HelpItem helpItem = tocAdapter.getItem(position);
			helpView.loadUrl("file:///android_asset/help/" + helpItem.language + "/" + helpItem.topic +".html");
			mDrawerLayout.closeDrawer(mDrawerList);
			mDrawerList.setSelected(false);
			getSupportActionBar().setTitle(getString(R.string.menu_help) + ": " + helpItem.topic);
		}
	}
	
	private class HelpViewWebViewClient extends WebViewClient {
		
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	// WebViewClient is slightly bizarre because there is no way to indicate to the webview that you would like 
	    	// if to process the url in its default way, its either handling it yourself or loading it directly into the 
	    	// webview
	    	if (url != null && url.startsWith("file:")) {
	    		Log.d("HelpViewer","orig " + url);
	    		getSupportActionBar().setTitle(getString(R.string.menu_help) + ": " + getTopic(url));
	    		if (url.endsWith(".md")) { // on device we have pre-generated html
	    			url = url.substring(0,url.length()-".md".length()) + ".html";
	    			Log.d("HelpViewer","new " + url);
	    		}
	    		view.loadUrl(url);
	    		return true;
	    	} else {
	    		view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));  
	    	    return true;
	    	}
	    }
	    
	    @Override
	    public void onPageFinished (WebView view, String url) {
	    	super.onPageFinished(view, url);
	    	if (url.startsWith("file:")) {
	    		getSupportActionBar().setTitle(getString(R.string.menu_help) + ": " + getTopic(url));
	    	}
	    }
	}
	
	private String getTopic(String url) {
		
		try {
			url = URLDecoder.decode(url,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			return "Error, got: " + url;
		}
		int lastSlash = url.lastIndexOf('/');
		int lastDot = url.lastIndexOf('.');
		if (lastSlash < 0 || lastDot < 0) {
			return "Error, got: " + url;
		}
		return url.substring(lastSlash+1,lastDot);
	}
}
