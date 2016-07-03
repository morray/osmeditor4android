package de.blau.android.services.util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.acra.ACRA;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.util.Log;
import de.blau.android.services.exceptions.EmptyCacheException;
import de.blau.android.views.util.OpenStreetMapViewConstants;

/**
 * The OpenStreetMapTileProviderDataBase contains a table with info for the available renderers and one for
 * the available tiles in the file system cache.<br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010
 * by Marcus Wolschon to be integrated into the de.blau.androin
 * OSMEditor. 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon <Marcus@Wolschon.biz
 */
public class OpenStreetMapTileProviderDataBase implements OpenStreetMapViewConstants {

	private static final String DATABASE_NAME = "osmaptilefscache_db";
	private static final int DATABASE_VERSION = 7;

	private static final String T_FSCACHE = "tiles";	
	private static final String T_FSCACHE_RENDERER_ID = "rendererID";
	private static final String T_FSCACHE_ZOOM_LEVEL = "zoom_level";
	private static final String T_FSCACHE_TILE_X = "tile_column";
	private static final String T_FSCACHE_TILE_Y = "tile_row";
//	private static final String T_FSCACHE_LINK = "link";			// TODO store link (multiple use for similar tiles)
	private static final String T_FSCACHE_TIMESTAMP = "timestamp";
	private static final String T_FSCACHE_USAGECOUNT = "countused";
	private static final String T_FSCACHE_FILESIZE = "filesize";
	private static final String T_FSCACHE_DATA = "tile_data";
	
	private static final String T_RENDERER               = "t_renderer";
	private static final String T_RENDERER_ID            = "id";
	private static final String T_RENDERER_NAME          = "name";
	private static final String T_RENDERER_BASE_URL      = "base_url";
	private static final String T_RENDERER_ZOOM_MIN      = "zoom_min";
	private static final String T_RENDERER_ZOOM_MAX      = "zoom_max";
	private static final String T_RENDERER_TILE_SIZE_LOG = "tile_size_log";
	
	private static final String T_FSCACHE_CREATE_COMMAND = "CREATE TABLE IF NOT EXISTS " + T_FSCACHE
	+ " (" 
	+ T_FSCACHE_RENDERER_ID + " VARCHAR(255) NOT NULL,"
	+ T_FSCACHE_ZOOM_LEVEL + " INTEGER NOT NULL,"
	+ T_FSCACHE_TILE_X + " INTEGER NOT NULL,"
	+ T_FSCACHE_TILE_Y + " INTEGER NOT NULL,"
	+ T_FSCACHE_TIMESTAMP + " DATE NOT NULL,"
	+ T_FSCACHE_USAGECOUNT + " INTEGER NOT NULL DEFAULT 1,"
	+ T_FSCACHE_FILESIZE + " INTEGER NOT NULL,"
	+ T_FSCACHE_DATA + " BLOB,"
	+ " PRIMARY KEY(" 	+ T_FSCACHE_RENDERER_ID + ","
						+ T_FSCACHE_ZOOM_LEVEL + ","
						+ T_FSCACHE_TILE_X + ","
						+ T_FSCACHE_TILE_Y + ")"
	+ ");";
	
	private static final String T_RENDERER_CREATE_COMMAND = "CREATE TABLE IF NOT EXISTS " + T_RENDERER
	+ " ("
	+ T_RENDERER_ID + " VARCHAR(255) PRIMARY KEY,"
	+ T_RENDERER_NAME + " VARCHAR(255),"
	+ T_RENDERER_BASE_URL + " VARCHAR(255),"
	+ T_RENDERER_ZOOM_MIN + " INTEGER NOT NULL,"
	+ T_RENDERER_ZOOM_MAX + " INTEGER NOT NULL,"
	+ T_RENDERER_TILE_SIZE_LOG + " INTEGER NOT NULL"
	+ ");";

	private static final String SQL_ARG = "=?";
	private static final String AND = " AND ";

	private static final String T_FSCACHE_WHERE = T_FSCACHE_RENDERER_ID + SQL_ARG + AND
												+ T_FSCACHE_ZOOM_LEVEL + SQL_ARG + AND
												+ T_FSCACHE_TILE_X + SQL_ARG + AND
												+ T_FSCACHE_TILE_Y + SQL_ARG;
	
	private static final String T_FSCACHE_WHERE_INVALID = T_FSCACHE_RENDERER_ID + SQL_ARG + AND
														+ T_FSCACHE_ZOOM_LEVEL + SQL_ARG + AND
														+ T_FSCACHE_TILE_X + SQL_ARG + AND
														+ T_FSCACHE_TILE_Y + SQL_ARG + AND
														+ T_FSCACHE_FILESIZE + "=0";
	
	private static final String T_FSCACHE_WHERE_NOT_INVALID = T_FSCACHE_RENDERER_ID + SQL_ARG + AND
			+ T_FSCACHE_ZOOM_LEVEL + SQL_ARG + AND
			+ T_FSCACHE_TILE_X + SQL_ARG + AND
			+ T_FSCACHE_TILE_Y + SQL_ARG + AND
			+ T_FSCACHE_FILESIZE + ">0";
	
	private static final String T_FSCACHE_SELECT_LEAST_USED = "SELECT " + T_FSCACHE_RENDERER_ID  + "," + T_FSCACHE_ZOOM_LEVEL + "," + T_FSCACHE_TILE_X + "," + T_FSCACHE_TILE_Y + "," + T_FSCACHE_FILESIZE + " FROM " + T_FSCACHE + " WHERE "  + T_FSCACHE_USAGECOUNT + " = (SELECT MIN(" + T_FSCACHE_USAGECOUNT + ") FROM "  + T_FSCACHE + ")";
	private static final String T_FSCACHE_SELECT_OLDEST = "SELECT " + T_FSCACHE_RENDERER_ID  + "," + T_FSCACHE_ZOOM_LEVEL + "," + T_FSCACHE_TILE_X + "," + T_FSCACHE_TILE_Y + "," + T_FSCACHE_FILESIZE + " FROM " + T_FSCACHE + " WHERE " + T_FSCACHE_FILESIZE + " > 0 ORDER BY " + T_FSCACHE_TIMESTAMP + " ASC";
	
	private static final String T_FSCACHE_INCREMENT_USE = "UPDATE " + T_FSCACHE +" SET " + T_FSCACHE_USAGECOUNT + "=" + T_FSCACHE_USAGECOUNT + "+1, " 
														+ T_FSCACHE_TIMESTAMP + "=" + SQL_ARG + " WHERE " + T_FSCACHE_WHERE;
	
	// ===========================================================
	// Fields
	// ===========================================================

	protected final Context mCtx;
	protected final OpenStreetMapTileFilesystemProvider mFSProvider;
	protected final SQLiteDatabase mDatabase;
	private final static String DATE_PATTERN_ISO8601_MILLIS = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	protected final SimpleDateFormat DATE_FORMAT_ISO8601 = new SimpleDateFormat(DATE_PATTERN_ISO8601_MILLIS, Locale.US);
	private final SQLiteStatement incrementUse;

	// ===========================================================
	// Constructors
	// ===========================================================

	public OpenStreetMapTileProviderDataBase(final Context context, OpenStreetMapTileFilesystemProvider openStreetMapTileFilesystemProvider) {
		Log.i("OSMTileProviderDB", "creating database instance");
		mCtx = context;
		mFSProvider = openStreetMapTileFilesystemProvider;
		mDatabase = new AndNavDatabaseHelper(context).getWritableDatabase();
		
		incrementUse = mDatabase.compileStatement(T_FSCACHE_INCREMENT_USE);
	}

	public boolean hasTile(final OpenStreetMapTile aTile) {
		boolean existed = false;
		if (mDatabase.isOpen()) {
			final String[] args = new String[]{"" + aTile.rendererID, "" + aTile.zoomLevel, "" + aTile.x, "" + aTile.y};
			final Cursor c = mDatabase.query(T_FSCACHE, new String[]{T_FSCACHE_RENDERER_ID}, T_FSCACHE_WHERE, args, null, null, null);
			existed = c.getCount() > 0;
			c.close();
		}
		return existed;
	}
	
	public boolean isInvalid(final OpenStreetMapTile aTile) {
		boolean existed = false;
		if (mDatabase.isOpen()) {
			final String[] args = new String[]{"" + aTile.rendererID, "" + aTile.zoomLevel, "" + aTile.x, "" + aTile.y};
			final Cursor c = mDatabase.query(T_FSCACHE, new String[]{T_FSCACHE_RENDERER_ID}, T_FSCACHE_WHERE_INVALID, args, null, null, null);
			existed = c.getCount() > 0;
			c.close();
		}
		return existed;
	}
	
	public boolean incrementUse(final OpenStreetMapTile aTile) {
		boolean ret = false;
		if (mDatabase.isOpen()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				try {
					incrementUse.bindString(1,getNowAsIso8601());
					incrementUse.bindString(2,aTile.rendererID);
					incrementUse.bindLong(3, aTile.zoomLevel);
					incrementUse.bindLong(4, aTile.x);
					incrementUse.bindLong(5, aTile.y);
					return incrementUse.executeUpdateDelete() >= 1; // > 1 is naturally an error, but safe to return true here
				} catch (Exception e) {
					ACRA.getErrorReporter().putCustomData("STATUS", "NOCRASH");
					ACRA.getErrorReporter().handleException(e);
					return true; // this will inidcate that the tile is in the DB which is erring on the safe side
				}
			} else {
				final String[] args = new String[] { "" + aTile.rendererID, "" + aTile.zoomLevel, "" + aTile.x,
						"" + aTile.y };
				Cursor c = mDatabase.query(T_FSCACHE, new String[] { T_FSCACHE_USAGECOUNT }, T_FSCACHE_WHERE, args, null,
						null, null);		
				if(DEBUGMODE) {
					Log.d(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "incrementUse found " + c.getCount() + " entries");
				}			
				if (c.getCount() == 1) {
					c.moveToFirst();
					try {
						int usageCount = c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_USAGECOUNT));
						ContentValues cv = new ContentValues();
						if(DEBUGMODE) {
							Log.d(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "incrementUse count " + usageCount);
						}	
						cv.put(T_FSCACHE_USAGECOUNT, usageCount + 1);
						cv.put(T_FSCACHE_TIMESTAMP, getNowAsIso8601());
						ret = mDatabase.update(T_FSCACHE, cv, T_FSCACHE_WHERE, args) > 0;
						if(DEBUGMODE) {
							Log.d(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "incrementUse count " + usageCount + " update sucessful " + ret);
						}
					} catch (Exception e) {
						if (e instanceof NullPointerException) {
							// just log ... likely these are really spurious
							Log.e(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "NPE in incrementUse");
						} else {
							ACRA.getErrorReporter().putCustomData("STATUS", "NOCRASH");
							ACRA.getErrorReporter().handleException(e);
						}
					}
				}
			}
		} else if(DEBUGMODE) {
			Log.d(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "incrementUse database not open");
		}	
		return ret;
	}

	public synchronized int addTileOrIncrement(final OpenStreetMapTile aTile, final byte[] tile_data) { 
		if(DEBUGMODE) {
			Log.d(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "adding or incrementing use " + aTile);
		}
		// there seems to be danger for  a race condition here
		if (incrementUse(aTile)) { // this should actually never be true
			if(DEBUGMODE) {
				Log.d(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "Tile existed");
			}
			return 0;
		} else {
			insertNewTileInfo(aTile, tile_data);
			return tile_data != null ? tile_data.length : 0;
		}
	}

	private void insertNewTileInfo(final OpenStreetMapTile aTile, final byte[] tile_data) {
		if(DEBUGMODE) {
			Log.d(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "Inserting new tile");
		}
		if (mDatabase.isOpen()) {
			final ContentValues cv = new ContentValues();
			cv.put(T_FSCACHE_RENDERER_ID, aTile.rendererID);
			cv.put(T_FSCACHE_ZOOM_LEVEL, aTile.zoomLevel);
			cv.put(T_FSCACHE_TILE_X, aTile.x);
			cv.put(T_FSCACHE_TILE_Y, aTile.y);
			cv.put(T_FSCACHE_TIMESTAMP, getNowAsIso8601());
			cv.put(T_FSCACHE_FILESIZE, tile_data != null ? tile_data.length : 0); // 0 == invalid
			cv.put(T_FSCACHE_DATA, tile_data);
			long result = mDatabase.insert(T_FSCACHE, null, cv);
			if(DEBUGMODE) {
				Log.d(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "Inserting new tile result " + result);
			}
		}
	}
	
	/**
	 * Returns requested tile and increases use count and date
	 * @param aTile
	 * @return the contents of the tile or null on failure to retrieve
	 * @throws IOException 
	 */
	public synchronized byte[] getTile(final OpenStreetMapTile aTile) throws IOException { 
		// there seems to be danger for  a race condition here
		if (DEBUGMODE) {
			Log.d(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "Trying to retrieve " + aTile + " from file");
		}
		if (incrementUse(aTile)) { // checks if DB is open
			final String[] args = new String[]{"" + aTile.rendererID, "" + aTile.zoomLevel, "" + aTile.x, "" + aTile.y};
			final Cursor c = mDatabase.query(T_FSCACHE, new String[] { T_FSCACHE_DATA }, T_FSCACHE_WHERE_NOT_INVALID, args, null, null, null);
			if (c.moveToFirst()) {
				byte[] tile_data = c.getBlob(c.getColumnIndexOrThrow(T_FSCACHE_DATA));
				c.close();
				if (DEBUGMODE) {
					Log.d(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "Sucessful retrieved " + aTile + " from file");
				}
				return tile_data;
			} else if(DEBUGMODE) {
				Log.d(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "Tile not found but should be 2");
			}
		}
		if(DEBUGMODE) {
			Log.d(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "Tile not found in DB");
		}
		return null;
	}
	
	long deleteOldest(final int pSizeNeeded) throws EmptyCacheException {
		if (!mDatabase.isOpen()) { // this seems to happen, protect against crashing
			Log.e(OpenStreetMapTileFilesystemProvider.DEBUGTAG,"deleteOldest called on closed DB");
			return 0;
		}
		final Cursor c = mDatabase.rawQuery(T_FSCACHE_SELECT_OLDEST, null);
		final ArrayList<OpenStreetMapTile> deleteFromDB = new ArrayList<OpenStreetMapTile>();
		long sizeGained = 0;
		if(c != null){
			OpenStreetMapTile tileToBeDeleted; 
			if(c.moveToFirst()){
				do{
					final int sizeItem = c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_FILESIZE));
					sizeGained += sizeItem;
					
					tileToBeDeleted = new OpenStreetMapTile(c.getString(c.getColumnIndexOrThrow(T_FSCACHE_RENDERER_ID)),c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_ZOOM_LEVEL)),
							c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_TILE_X)),c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_TILE_Y)));

					deleteFromDB.add(tileToBeDeleted);
					Log.d("OpenStreetMapTileProvierDatabase","deleteOldest " + tileToBeDeleted.toString());
					
				}while(c.moveToNext() && sizeGained < pSizeNeeded);
			}else{
				c.close();
				throw new EmptyCacheException("Cache seems to be empty.");
			}
			c.close();

			for(OpenStreetMapTile t : deleteFromDB) {
				final String[] args = new String[]{"" + t.rendererID, "" + t.zoomLevel, "" + t.x, "" + t.y};
				try {
					if (mDatabase.isOpen()) { // note we have already deleted the on disks tiles so it is not really an issue if we don't delete everything from the DB
						mDatabase.delete(T_FSCACHE, T_FSCACHE_WHERE, args);
					}
				} catch (Exception e) {
					if (e instanceof NullPointerException) {
						// just log ... likely these are really spurious
						Log.e(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "NPE in deleteOldest");
					} else {
						ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
						ACRA.getErrorReporter().handleException(e);	
					}
				}
			}
		}
		return sizeGained;
	}
	
	/**
	 * Delete all tiles from cache for a specific renderer
	 * @param rendererID
	 * @throws EmptyCacheException
	 */
	public void flushCache(String rendererID) throws EmptyCacheException {
		Log.d(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "Flushing cache for " + rendererID); 
		final Cursor c = mDatabase.rawQuery("SELECT " + T_FSCACHE_ZOOM_LEVEL + "," + T_FSCACHE_TILE_X + "," + T_FSCACHE_TILE_Y + "," + T_FSCACHE_FILESIZE + " FROM " + T_FSCACHE + " WHERE " + T_FSCACHE_RENDERER_ID + "='" + rendererID + "' ORDER BY " + T_FSCACHE_TIMESTAMP + " ASC", null);
		final ArrayList<OpenStreetMapTile> deleteFromDB = new ArrayList<OpenStreetMapTile>();
		long sizeGained = 0;
		if(c != null){
			OpenStreetMapTile tileToBeDeleted; 
			if(c.moveToFirst()){
				do{
					final int sizeItem = c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_FILESIZE));
					sizeGained += sizeItem;
					
					tileToBeDeleted = new OpenStreetMapTile(rendererID,c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_ZOOM_LEVEL)),
							c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_TILE_X)),c.getInt(c.getColumnIndexOrThrow(T_FSCACHE_TILE_Y)));
			
					deleteFromDB.add(tileToBeDeleted);
					Log.d("OpenStreetMapTileProvierDatabase","flushCache " + tileToBeDeleted.toString());
				}while(c.moveToNext());
			}else{
				c.close();
				throw new EmptyCacheException("Cache seems to be empty.");
			}
			c.close();

			for(OpenStreetMapTile t : deleteFromDB) {
				final String[] args = new String[]{"" + t.rendererID, "" + t.zoomLevel, "" + t.x, "" + t.y};
				mDatabase.delete(T_FSCACHE, T_FSCACHE_WHERE, args);
			}
			// FIXME vacuuming might be a good idea
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================
	private String TMP_COLUMN = "tmp"; 
	public int getCurrentFSCacheByteSize() {
		int ret = 0;
		if (mDatabase.isOpen()) {
			final Cursor c = mDatabase.rawQuery("SELECT SUM(" + T_FSCACHE_FILESIZE + ") AS " + TMP_COLUMN + " FROM " + T_FSCACHE, null);
			if(c != null){
				if(c.moveToFirst()){
					ret = c.getInt(c.getColumnIndexOrThrow(TMP_COLUMN));
				}
				c.close();
			}
		}

		return ret;
	}

	/**
	 * Get at the moment within ISO8601 format.
	 * @return
	 * Date and time in ISO8601 format.
	 */
	private String getNowAsIso8601() {
		return DATE_FORMAT_ISO8601.format(new Date());
	} 

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	private class AndNavDatabaseHelper extends SQLiteOpenHelper {
		AndNavDatabaseHelper(final Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			try {
				db.execSQL(T_RENDERER_CREATE_COMMAND);
				db.execSQL(T_FSCACHE_CREATE_COMMAND);
			} catch (SQLException e) {
				Log.w(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "Problem creating database", e);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if(DEBUGMODE)
				Log.w(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");

			db.execSQL("DROP TABLE IF EXISTS " + T_FSCACHE);

			onCreate(db);
		}
	}

	/**
	 * Close the DB handle
	 */
	public void close() {
		mDatabase.close();
	}

	/**
	 * Deletes the database
	 * @param context
	 */
	public static void delete(final Context context) {
		Log.w(OpenStreetMapTileFilesystemProvider.DEBUGTAG, "Deleting database " + DATABASE_NAME);
		context.deleteDatabase(DATABASE_NAME);
	}

	/**
	 * Check if the database exist and can be read.
	 * 
	 * @return true if it exists and can be read, false if it doesn't
	 */
	public static boolean exists(File dir) {
	    SQLiteDatabase checkDB = null;
	    try {
	    	String path = dir.getAbsolutePath() + "/databases/" + DATABASE_NAME + ".db";  
	        checkDB = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
	        checkDB.close();
	    } catch (SQLiteException e) {
	        // database doesn't exist yet.
	    }
	    return checkDB != null;
	}
}