package de.blau.android.propertyeditor;


import java.util.ArrayList;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.view.ActionMode.Callback;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class SelectedRowsActionModeCallback implements Callback {

	public interface Row {

		void delete();

		void deselect();

		boolean isSelected();
	}
	
	protected static final int MENU_ITEM_DELETE = 1;
	protected static final int MENU_ITEM_HELP = 15;

	ActionMode currentAction;

	LinearLayout rows = null;
	Fragment caller = null;

	public SelectedRowsActionModeCallback(Fragment caller, LinearLayout rows) {
		this.rows = rows;
		this.caller = caller;
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		currentAction = mode;
		((PropertyEditor)caller.getActivity()).disablePaging();
		((PropertyEditor)caller.getActivity()).disablePresets();
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		menu.clear();
		Context context = caller.getActivity();
		menu.add(Menu.NONE, MENU_ITEM_DELETE, Menu.NONE, R.string.delete)
				.setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_delete));
		menu.add(EasyEditManager.GROUP_BASE, MENU_ITEM_HELP, Menu.CATEGORY_SYSTEM, R.string.menu_help)
				.setAlphabeticShortcut(Util.getShortCut(context, R.string.shortcut_help))
				.setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_help));
		return true;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_DELETE:
			final int size = rows.getChildCount();
			ArrayList<Row> toDelete = new ArrayList<Row>();
			for (int i = 0; i < size; ++i) {
				View view = rows.getChildAt(i);
				Row row = (Row)view;
				if (row.isSelected()) {
					toDelete.add(row);
				}
			}
			if (toDelete.size() > 0) {
				for (Row r : toDelete) {
					r.deselect();
					r.delete();
				}
			}
			if (currentAction != null) {
				currentAction.finish();
			}
			break;
		case MENU_ITEM_HELP:
			HelpViewer.start(caller.getActivity(), R.string.help_propertyeditor);
			return true;
		default: return false;
		}
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		final int size = rows.getChildCount();
		for (int i = 0; i < size; ++i) {
			View view = rows.getChildAt(i);
			Row row = (Row)view;
			row.deselect();
		}
		((PropertyEditor)caller.getActivity()).enablePaging();
		((PropertyEditor)caller.getActivity()).enablePresets();
		PropertyRows rowContainer = (PropertyRows)caller;
		rowContainer.deselectHeaderCheckBox();
		rowContainer.deselectRow();
		currentAction = null;
		((AppCompatActivity)caller.getActivity()).supportInvalidateOptionsMenu();
	}

	public boolean rowsDeselected(boolean skipHeaderRow) {
		final int size = rows.getChildCount();
		int initialRowIndex = skipHeaderRow ? 1 : 0;
		for (int i = initialRowIndex; i < size; ++i) {
			View view = rows.getChildAt(i);
			Row row = (Row)view;
			if (row.isSelected()) {
				// something is still selected
				return false;
			}
		}
		// nothing selected -> finish
		if (currentAction != null) {
			currentAction.finish();
		}
		return true;
	}

	public void invalidate() {
		if (currentAction != null) {
			currentAction.invalidate();
		}
	}

}
