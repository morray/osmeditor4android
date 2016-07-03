/**
 * TagKeyAutocompletionAdapter.java
 * created: 12.06.2010 10:43:37
 * (c) 2010 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
 * This file is part of OSMEditor by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  OSMEditor is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  OSMEditor is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OSMEditor.  If not, see <http://www.gnu.org/licenses/>.
 *
 ***********************************
 * Editing this file:
 *  -For consistent code-quality this file should be checked with the
 *   checkstyle-ruleset enclosed in this project.
 *  -After the design of this file has settled it should get it's own
 *   JUnit-Test that shall be executed regularly. It is best to write
 *   the test-case BEFORE writing this class and to run it on every build
 *   as a regression-test.
 */
package de.blau.android.util;

//other imports
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.propertyeditor.PropertyEditor;


/**
 * Project: OSMEditor<br/>
 * TagKeyAutocompletionAdapter.java<br/>
 * created: 12.06.2010 10:43:37 <br/>
 *<br/><br/>
 * <b>Adapter for the {@link AutoCompleteTextView} in the {@link PropertyEditor}
 * that is for the VALUE  for the key "addr:street" .</a>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class PlaceTagValueAdapter extends ArrayAdapter<ValueWithCount> {

    /**
     * The tag we use for Android-logging.
     */
    @SuppressWarnings("unused")
	private static final String DEBUG_TAG = PlaceTagValueAdapter.class.getName();

    ElementSearch es;
    
    /**
     * 
     * @param aContext used to load resources
     * @param aTextViewResourceId given to {@link ArrayAdapter}
     * @param osmId 
     * @param type 
     */
    public PlaceTagValueAdapter(final Context aContext,
                                       final int aTextViewResourceId,
                                       final StorageDelegator delegator,
                                       final String osmElementType,
                                       final long osmId,
                                       ArrayList<String> extraValues) {
        super(aContext, aTextViewResourceId);
        Log.d("PlaceTagValuesCompletionAdapter","Constructor ...");
        
        HashMap<String, Integer> counter = new HashMap<String, Integer>();
        if (extraValues != null && extraValues.size() > 0) {
        	for(String t:extraValues) {
        		if (t.equals("")) {
        			continue;
        		}
        		if (counter.containsKey(t)) {
        			counter.put(t, Integer.valueOf(counter.get(t).intValue()+1));
        		} else {
        			counter.put(t, Integer.valueOf(1));
        		}
        	}
        	ArrayList<String> keys = new ArrayList<String>(counter.keySet());
        	Collections.sort(keys);
        	for (String t:keys) {
        		ValueWithCount v = new ValueWithCount(t,counter.get(t).intValue());
            	super.add(v);
        	}
        }
        
        es = new ElementSearch(Util.getCenter(delegator, osmElementType, osmId), false);
        for (String s:es.getPlaceNames()) {
           	if (counter.size()> 0 && counter.containsKey(s)) {
        		continue; // skip values that we already have
        	}
        	ValueWithCount v = new ValueWithCount(s);
        	super.add(v);
        }
    }
    
    public String[] getNames() {
    	return es.getPlaceNames();
    }
    
    public long getId(String name) throws OsmException {
    	return es.getPlaceId(name);
    }
 
    /**
     * This avoids generating everything twice
     * @return
     */
    public ElementSearch getElementSearch() {
    	return es;
    }
}


