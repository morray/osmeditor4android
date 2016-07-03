package de.blau.android.tasks;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import de.blau.android.Application;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import de.blau.android.listener.UpdateViewListener;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.Preferences;
import de.blau.android.tasks.Task.State;
import de.blau.android.util.GeoMath;
import de.blau.android.util.IssueAlert;

/**
 * Very simple dialog fragment to display bug or notes
 * @author simon
 *
 */
public class TaskFragment extends DialogFragment {
	private static final String DEBUG_TAG = TaskFragment.class.getSimpleName();
	 
	UpdateViewListener mListener;

    /**
     */
    static public TaskFragment newInstance(Task b) {
    	TaskFragment f = new TaskFragment();

        Bundle args = new Bundle();
        args.putSerializable("bug", b);

        f.setArguments(args);
        f.setShowsDialog(true);
        
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @SuppressLint({ "NewApi", "InflateParams" })
	@Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
    	final Task bug = (Task) getArguments().getSerializable("bug");
    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    	// Get the layout inflater
    	LayoutInflater inflater = getActivity().getLayoutInflater();

    	final Preferences prefs = new Preferences(getActivity());
    	
    	// Inflate and set the layout for the dialog
    	// Pass null as the parent view because its going in the dialog layout
    	final View v = inflater.inflate(R.layout.openstreetbug_edit, null);
    	builder.setView(v)
    		// Add action buttons - slightly convoluted 
    		.setPositiveButton(bug instanceof Note && bug.isNew() ? (Application.getTaskStorage().contains(bug) ? R.string.delete : R.string.openstreetbug_commitbutton): R.string.save, new DialogInterface.OnClickListener() { 
    			public void onClick(DialogInterface dialog, int id) {
      				if (bug instanceof Note && bug.isNew() && Application.getTaskStorage().contains(bug)) {
    					deleteBug(bug);
    					return;
    				}
      				saveBug(v,bug);
    				if (bug.hasBeenChanged() && bug.isClosed()) {
    					IssueAlert.cancel(getActivity(), bug);
    				}
    			}
    		})
    		.setNeutralButton(R.string.transfer_download_current_upload, new DialogInterface.OnClickListener() { 
    			public void onClick(DialogInterface dialog, int id) {
    				saveBug(v,bug);
    				if (bug instanceof Note) {
    					Note n = (Note)bug;
    					NoteComment nc = n.getLastComment();
    					TransferTasks.uploadNote(getActivity(), prefs.getServer(), n, (nc != null && nc.isNew()) ? nc.getText() : null, n.state == State.CLOSED, false);
    				} else if (bug instanceof OsmoseBug) {
    					TransferTasks.uploadOsmoseBug((OsmoseBug)bug);
    				}
    				if (bug.hasBeenChanged() && bug.isClosed()) {
    					IssueAlert.cancel(getActivity(), bug);
    				}
    			}
    		})
    		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int id) {
    			}
    		});

    	final Spinner state = (Spinner)v.findViewById(R.id.openstreetbug_state);
    	ArrayAdapter<CharSequence> adapter = null;

    	TextView comments = (TextView)v.findViewById(R.id.openstreetbug_comments);
    	EditText comment = (EditText)v.findViewById(R.id.openstreetbug_comment);
    	TextView commentLabel = (TextView)v.findViewById(R.id.openstreetbug_comment_label);
    	LinearLayout elementLayout = (LinearLayout)v.findViewById(R.id.openstreetbug_element_layout);
    	if (bug instanceof Note) {
    		builder.setTitle(getString((bug.isNew() && ((Note)bug).count() == 0) ? R.string.openstreetbug_new_title : R.string.openstreetbug_edit_title));  		
    		comments.setText(Html.fromHtml(((Note)bug).getComment())); // ugly	
    		comments.setAutoLinkMask(Linkify.WEB_URLS);
    		comments.setMovementMethod(LinkMovementMethod.getInstance()); 
    		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
    			comments.setTextIsSelectable(true);
    		}
    		NoteComment nc = ((Note) bug).getLastComment();	
    		elementLayout.setVisibility(View.GONE); // not used for notes
    		if ((bug.isNew() && ((Note)bug).count() == 0) || (nc != null && !nc.isNew())) { // only show comment field if we don't have an unsaved comment
    			Log.d(DEBUG_TAG,"enabling comment field");
    			comment.setText("");
    			comment.setFocusable( true);
    			comment.setFocusableInTouchMode(true);
    			comment.setEnabled(true);
    		} else {
        		commentLabel.setVisibility(View.GONE);
        		comment.setVisibility(View.GONE);
    		}
    		adapter = ArrayAdapter.createFromResource(getActivity(),
        	        R.array.note_state, android.R.layout.simple_spinner_item);
    	} else if (bug instanceof OsmoseBug) {
    		builder.setTitle(R.string.openstreetbug_bug_title);
    		comments.setText(Html.fromHtml(((OsmoseBug)bug).getLongDescription(getActivity(), false)));
    		final StorageDelegator storageDelegator = Application.getDelegator();
    		for (final OsmElement e:((OsmoseBug)bug).getElements()) {
    			String text;
    			if (e.getOsmVersion() < 0) { // fake element
    				text = e.getName() + " (" + getActivity().getString(R.string.openstreetbug_not_downloaded) + ") #" + e.getOsmId();
    			} else { // real
    				text = e.getName() + " " + e.getDescription(false);
    			}
    			TextView tv = new TextView(getActivity());
    			tv.setClickable(true);
    			tv.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) { // FIXME assumption that we are being called from Main
						dismiss();
						final int lonE7 = bug.getLon();
						final int latE7 = bug.getLat();
						if (e.getOsmVersion() < 0) { // fake element
							try {
								BoundingBox b = GeoMath.createBoundingBoxForCoordinates(latE7/1E7D, lonE7/1E7, 50, true);
								Application.getLogic().downloadBox(b, true, new PostAsyncActionHandler(){
									@Override
									public void execute(){
										OsmElement osm = storageDelegator.getOsmElement(e.getName(), e.getOsmId());
										if (osm != null) {
											Application.mainActivity.zoomToAndEdit(lonE7, latE7, osm);
										}
									}
								});
							} catch (OsmException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
		    			} else { // real
		    				Application.mainActivity.zoomToAndEdit(lonE7, latE7, e);
		    			}
					}});
    			tv.setTextColor(ContextCompat.getColor(getActivity(),R.color.holo_blue_light));
    			tv.setText(text);
    			elementLayout.addView(tv);
    		}
    		// these are not used for osmose bugs
    		commentLabel.setVisibility(View.GONE);
    		comment.setVisibility(View.GONE);
    		//
    		adapter = ArrayAdapter.createFromResource(getActivity(),
        	        R.array.bug_state, android.R.layout.simple_spinner_item);
    	} else {
    		// unknown bug type
    		Log.d(DEBUG_TAG, "Unknown bug type " + bug.getDescription());
    		return null;
    	}

    	// Specify the layout to use when the list of choices appears
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
       	// Apply the adapter to the spinner
    	state.setAdapter(adapter);
    	
    	if (bug.state == State.OPEN) {
    		state.setSelection(Task.POS_OPEN);
    	} else if (bug.state == State.CLOSED) {
    		state.setSelection(Task.POS_CLOSED);
    	} else if (bug.state == State.FALSE_POSITIVE) {
    		if (adapter.getCount() == 3) {
    			state.setSelection(Task.POS_FALSE_POSITIVE);
    		} else {
    			Log.d(DEBUG_TAG, "ArrayAdapter too short");
    		}
    	} 
    	
    	state.setEnabled(!bug.isNew()); // new bugs always open
    	AppCompatDialog d = builder.create();
    	
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
    		d.setOnShowListener(new OnShowListener() { // old API, buttons are enabled by default
    			@Override
    			public void onShow(DialogInterface dialog) {                    //
    				final Button save = ((AlertDialog) dialog)
    						.getButton(AlertDialog.BUTTON_POSITIVE);
    				if ((bug instanceof Note && bug.isNew() && ((Note)bug).count() == 1 && !Application.getTaskStorage().contains(bug)) || !bug.hasBeenChanged()) {
    					save.setEnabled(false);
    				}
    				final Button upload = ((AlertDialog) dialog)
    						.getButton(AlertDialog.BUTTON_NEUTRAL);
    				if (!bug.hasBeenChanged()) {
    					upload.setEnabled(false);
    				}
    				state.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
    					@Override
    					public void onItemSelected(AdapterView<?> arg0, View arg1,
    							int arg2, long arg3) {
    						save.setEnabled(true);
    						upload.setEnabled(true);	
    					}

    					@Override
    					public void onNothingSelected(AdapterView<?> arg0) {
    					}
    				});
    				EditText comment = (EditText)v.findViewById(R.id.openstreetbug_comment);
    				comment.addTextChangedListener(new TextWatcher(){
    					@Override
    					public void afterTextChanged(Editable arg0) {
    					}

    					@Override
    					public void beforeTextChanged(CharSequence s, int start,
    							int count, int after) {
    					}

    					@Override
    					public void onTextChanged(CharSequence s, int start,
    							int before, int count) {
    						save.setEnabled(true);
    						upload.setEnabled(true);
    						state.setSelection(Task.POS_OPEN);
    					}    				
    				});
    			}
    		});
    	} 
		d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    	return d;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(DEBUG_TAG, "onAttach");
        try {
            mListener = (UpdateViewListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPresetSelectedListener");
        }
    }
    
    @Override
    public void onDismiss(DialogInterface dialog) {
    	super.onDismiss(dialog);
    	if (mListener != null) {
    		mListener.update();
    	}
    }
    
    public static State pos2state(int pos) {
		if (pos == Task.POS_CLOSED) {
			return State.CLOSED;
		} else if (pos == Task.POS_OPEN) {
			return State.OPEN;
		} else if (pos == Task.POS_FALSE_POSITIVE) {
			return State.FALSE_POSITIVE;
		}
		return State.OPEN;
    }
    
    /** 
     * saves bug to storage if it is new, otherwise update comment and/or state
     * @param v
     * @param bug
     */
    void saveBug(View v, Task bug) {
    	if (bug.isNew() && ((Note)bug).count() == 0) {
			Application.getTaskStorage().add(bug); // sets dirty
		}
		String c = ((EditText)v.findViewById(R.id.openstreetbug_comment)).getText().toString();
		if (c.length() > 0) {
			((Note)bug).addComment(c);
		}
		final Spinner state = (Spinner)v.findViewById(R.id.openstreetbug_state);
		bug.state = pos2state(state.getSelectedItemPosition());
		bug.changed = true;
		Application.getTaskStorage().setDirty();
    }
    
    /**
     * Delete a new, non-saved, bug from storage
     * @param bug
     */
    void deleteBug(Task bug) {
    	if (bug.isNew()) {
			Application.getTaskStorage().delete(bug); // sets dirty
		}
    }
}
