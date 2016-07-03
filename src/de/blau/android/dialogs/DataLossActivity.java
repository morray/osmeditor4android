package de.blau.android.dialogs;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog asking for confirmation before starting an activity that might result in data loss.
 *
 */
public class DataLossActivity extends DialogFragment
{
	
	private static final String DEBUG_TAG = DataLossActivity.class.getSimpleName();
	
	private static final String TAG = "fragment_dataloss_activity";
		
	private Intent intent;
	private int requestCode;
	
   	/**
	 * Shows a dialog warning the user that he has unsaved changes that will be discarded.
	 * @param activity Activity creating the dialog and starting the intent Activity if confirmed
	 * @param c class for the Activity to start on confirmation
	 * @param requestCode If the activity should return a result, a non-negative request code.
	 *                    If no result is expected, set to -1.
	 */
	static public void showDialog(FragmentActivity activity, final Intent intent, final int requestCode) {
		dismissDialog(activity);

		FragmentManager fm = activity.getSupportFragmentManager();
	    DataLossActivity dataLossActivityFragment = newInstance(intent, requestCode);
	    if (dataLossActivityFragment != null) {
	    	dataLossActivityFragment.show(fm, TAG);
	    } else {
	    	Log.e(DEBUG_TAG,"Unable to create dataloss activity dialog ");
	    }
	}
	
	static public void dismissDialog(FragmentActivity activity) {
		FragmentManager fm = activity.getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
	    Fragment fragment = fm.findFragmentByTag(TAG);
	    if (fragment != null) {
	        ft.remove(fragment);
	    }
	    ft.commit();
	}
		
    /**
     */
    static private DataLossActivity newInstance(final Intent intent, final int requestCode) {
    	DataLossActivity f = new DataLossActivity();

        Bundle args = new Bundle();
        args.putParcelable("intent", intent);
        args.putInt("requestcode", requestCode);

        f.setArguments(args);
        f.setShowsDialog(true);
        
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setCancelable(true);
        intent = getArguments().getParcelable("intent");
        requestCode = getArguments().getInt("requestcode");
    }

    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState)
    {
    	Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(),R.attr.alert_dialog));
    	builder.setTitle(R.string.unsaved_data_title);
    	builder.setMessage(R.string.unsaved_data_message);
    	builder.setPositiveButton(R.string.unsaved_data_proceed,
    		new OnClickListener() {
   				@Override
   				public void onClick(DialogInterface dialog, int which) {
   					getActivity().startActivityForResult(intent, requestCode);
    			}
    		}
    	);
    	builder.setNegativeButton(R.string.cancel, null);

    	return builder.create();
    }	
}
