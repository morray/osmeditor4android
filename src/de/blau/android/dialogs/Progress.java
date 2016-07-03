package de.blau.android.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.blau.android.R;
import de.blau.android.util.ThemeUtils;

/**
 * ProgressDialog can't be styled, this rolls its own.
 * 
 * @author simon
 *
 */
public class Progress extends DialogFragment
{
	
	private static final String DEBUG_TAG = Progress.class.getSimpleName();
	
	public static final int PROGRESS_LOADING = 1;
	
	public static final int PROGRESS_DOWNLOAD = 2;
	
	public static final int PROGRESS_DELETING = 3;
	
	public static final int PROGRESS_SEARCHING = 4;
	
	public static final int PROGRESS_SAVING = 5;
	
	public static final int PROGRESS_OAUTH = 6;
	
	public static final int PROGRESS_UPLOADING = 7;
	
	public static final int PROGRESS_PRESET = 8;
	
	private int titleId;
	private int messageId;
	
	static public void showDialog(FragmentActivity activity, int dialogType) {
		showDialog(activity, dialogType, null);
	}
	
	static public void showDialog(FragmentActivity activity, int dialogType, String tag) {
		dismissDialog(activity, dialogType, tag);

		FragmentManager fm = activity.getSupportFragmentManager();
	    Progress progressDialogFragment = newInstance(dialogType);
	    try {
	    	tag = getTag(dialogType) + (tag != null?"-"+tag:"");
	    	if (progressDialogFragment != null) {
	    		progressDialogFragment.show(fm, tag);
	    	} else {
	    		Log.e(DEBUG_TAG,"Unable to create dialog for value " + dialogType);
	    	}
	    } catch (IllegalStateException isex) {
	    	Log.e(DEBUG_TAG,"showDialog",isex);
	    }
	}
	
	static public void dismissDialog(FragmentActivity activity, int dialogType) {
		dismissDialog(activity, dialogType,null);
	}
	
	static public void dismissDialog(FragmentActivity activity, int dialogType, String tag) {
		FragmentManager fm = activity.getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
    	tag = getTag(dialogType) + (tag != null?"-"+tag:"");
	    Fragment fragment = fm.findFragmentByTag(tag);
	    try {
	    	if (fragment != null) {
	    		ft.remove(fragment);
		    	ft.commit(); 
	    	}
	    } catch (IllegalStateException isex) {
	    	Log.e(DEBUG_TAG,"dismissDialog",isex);
	    	// this is in general harmless and there is no real way of avoiding this 
	    	// except not to use progress dialogs at all, which however doesn't give
	    	// the user any reasonable amount of feed back
	    }
	}
	
	/**
	 * Dismiss all possible progress dialogs to stop them being recreated
	 * @param activity
	 */
	static public void dismissAll(FragmentActivity activity) {
		dismissDialog(activity,PROGRESS_LOADING);
		dismissDialog(activity,PROGRESS_DOWNLOAD);
		dismissDialog(activity,PROGRESS_DELETING);
		dismissDialog(activity,PROGRESS_SEARCHING);
		dismissDialog(activity,PROGRESS_SAVING);
		dismissDialog(activity,PROGRESS_OAUTH);
		dismissDialog(activity,PROGRESS_UPLOADING);
		dismissDialog(activity,PROGRESS_PRESET);
	}
	
	private static String getTag(int dialogType) {
		switch (dialogType) {
		case PROGRESS_LOADING:
			return "dialog_progress_loading";
		case PROGRESS_DOWNLOAD:
			return "dialog_progress_download";
		case PROGRESS_DELETING:
			return "dialog_progress_deleting";
		case PROGRESS_SEARCHING:
			return "dialog_progress_searching";
		case PROGRESS_SAVING:
			return "dialog_progress_saving";
		case PROGRESS_OAUTH:
			return "dialog_progress_oauth";
		case PROGRESS_UPLOADING:
			return "dialog_progress_uploading";
		case PROGRESS_PRESET:
			return "dialog_progress_preset";
		}
		return null;
	}
	
	static private Progress newInstance(int dialogType) {
		switch (dialogType) {
		case PROGRESS_LOADING:
			return createNewInstance(R.string.progress_message);
		case PROGRESS_DOWNLOAD:
			return createNewInstance(R.string.progress_download_message);
		case PROGRESS_DELETING:
			return createNewInstance(R.string.progress_general_title, R.string.progress_deleting_message);
		case PROGRESS_SEARCHING:
			return createNewInstance(R.string.progress_general_title, R.string.progress_searching_message);
		case PROGRESS_SAVING:
			return createNewInstance(R.string.progress_general_title, R.string.progress_saving_message);
		case PROGRESS_OAUTH:
			return createNewInstance(R.string.progress_general_title, R.string.progress_oauth);
		case PROGRESS_UPLOADING:
			return createNewInstance(R.string.progress_general_title, R.string.progress_uploading_message);
		case PROGRESS_PRESET:
			return createNewInstance(R.string.progress_general_title, R.string.progress_preset_message);
		}
		return null;
	}
	
    /**
     */
    static private Progress createNewInstance(final int messageId) {
    	return createNewInstance(R.string.progress_title, messageId);
    }
	
    static private Progress createNewInstance(final int titleId, final int messageId) {
    	Progress f = new Progress();

        Bundle args = new Bundle();
        args.putSerializable("title", titleId);
        args.putSerializable("message", messageId);

        f.setArguments(args);
        f.setShowsDialog(true);
        
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setCancelable(true);
        titleId = (Integer) getArguments().getSerializable("title");
        messageId = (Integer) getArguments().getSerializable("message");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
		Context ctx = getActivity();
		
	   	// inflater needs to be got from a themed view or else all our custom stuff will not style correctly
    	final LayoutInflater inflater = ThemeUtils.getLayoutInflater(ctx);
    	
    	Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setTitle(titleId);
    	
		View layout = inflater.inflate(R.layout.progress, null);
		TextView message = (TextView) layout.findViewById(R.id.progressMessage);
		message.setText(messageId);
		ProgressBar progressBar = (ProgressBar) layout.findViewById(R.id.progressBar);
		if (progressBar.getIndeterminateDrawable() != null) {
			PorterDuff.Mode mode = android.graphics.PorterDuff.Mode.SRC_IN;
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				mode = android.graphics.PorterDuff.Mode.MULTIPLY; // ugly but at least it animates
			}
	        progressBar.getIndeterminateDrawable().setColorFilter(ThemeUtils.getStyleAttribColorValue(ctx, R.attr.colorAccent, 0), mode);
		}
		builder.setView(layout);
               
        return builder.create();
    }
}
