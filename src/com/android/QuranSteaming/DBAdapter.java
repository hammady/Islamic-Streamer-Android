package com.android.QuranSteaming;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
//import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

public class DBAdapter{
	 
    //The Android's default system path of your application database.
    private static String DB_PATH = "/data/data/com.android.QuranSteaming/databases/";
 
    private static String DB_NAME = "quranstreamingDB";
    private static final String TAG = "DBAdapter";    
    private static final int DATABASE_VERSION = 16; 
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;
    private static Context context;
    
    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     * @param context
     */
    public DBAdapter(Context ctx) {
 
    	DBAdapter.context = ctx;
        DBHelper = new DatabaseHelper(context);        
    }
    private static class DatabaseHelper extends SQLiteOpenHelper 
    {
        DatabaseHelper(Context context) 
        {
            super(context, DB_NAME, null, DATABASE_VERSION);
        }

        @Override
    	public void onCreate(SQLiteDatabase db) {
    		try
    		{
    		//db.execSQL("CREATE TABLE channel_schedule (_id INTEGER PRIMARY KEY, from_type TEXT, to_time TEXT, title TEXT, details TEXT)");
    		//db.execSQL("CREATE TABLE channels (channelname TEXT, _id INTEGER PRIMARY KEY, network TEXT)");
    		//db.execSQL("CREATE TABLE networks (networkname TEXT, _id INTEGER PRIMARY KEY, is_favourite TEXT, is_preferred TEXT)");
    		} 
    		catch(SQLiteException ex)
    		{
    			throw new Error(ex.getMessage());
    		}
    		catch(Exception ex){
    			throw new Error(ex.getMessage());
    		}
    	}

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion,int newVersion) 
        {
            Log.w(TAG, "Upgrading database from version " + oldVersion 
                    + " to "
                    + newVersion + ", which will destroy all old data");
            //db.execSQL("DROP TABLE IF EXISTS networks");
            //db.execSQL("DROP TABLE IF EXISTS channels");
            //db.execSQL("DROP TABLE IF EXISTS channel_schedule");
            //onCreate(db);
            
            try 
            {
    			context.deleteDatabase(DB_NAME);
    		} 
            catch (Exception e) 
            {
        		throw new Error("Error copying database");
        	}
        }
    }    
  /**
     * Creates a empty database on the system and rewrites it with your own database.
     * */
    public void createDataBase() throws IOException{
 
    	boolean dbExist = checkDataBase();
 
    	if(dbExist){
    		//do nothing - database already exist
    		DBHelper.getWritableDatabase();
    		DBHelper.close();
    	}
    	dbExist = checkDataBase();
    	if(!dbExist)
    	{
    		//By calling this method and empty database will be created into the default system path
               //of your application so we are gonna be able to overwrite that database with our database.
    		DBHelper.getReadableDatabase();
 
        	try 
        	{
    			copyDataBase();
    		} catch (IOException e) 
    		{
        		throw new Error("Error copying database");
        	}
    	}
    }
 
    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     * @return true if it exists, false if it doesn't
     */
    private boolean checkDataBase(){
     	SQLiteDatabase checkDB = null;
     	try{
    		String myPath = DB_PATH + DB_NAME;
    		checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
     	}catch(SQLiteException e){
     		//database does't exist yet.
     	}
     	if(checkDB != null){
     		checkDB.close();
     	}
     	return checkDB != null ? true : false;
    }
 
    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     * */
    private static void copyDataBase() throws IOException{
     	//Open your local db as the input stream
    	InputStream myInput = context.getAssets().open(DB_NAME);
     	// Path to the just created empty db
    	String outFileName = DB_PATH + DB_NAME;
     	//Open the empty db as the output stream
    	OutputStream myOutput = new FileOutputStream(outFileName);
     	//transfer bytes from the inputfile to the outputfile
    	byte[] buffer = new byte[1024];
    	int length;
    	while ((length = myInput.read(buffer))>0){
    		myOutput.write(buffer, 0, length);
    	}
     	//Close the streams
    	myOutput.flush();
    	myOutput.close();
    	myInput.close();
     }
 
    public void openDataBase() throws SQLiteException{
     	//Open the database
        String myPath = DB_PATH + DB_NAME;
        setDb(SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE));
    }	
    
  //---closes the database---    
    public void close() 
    {
        DBHelper.close();
    }
  
        // Add your public helper methods to access and get content from the database.
       // You could return cursors by doing "return myDataBase.query(....)" so it'd be easy
       // to you to create adapters for your views.
    
	public Cursor getData(String sql,String[] columns,String filter,String type,String sortCol)
    {
		Cursor cr=null;
		if(type=="select")
			cr =getDb().query(sql, columns, filter,null,null,null,sortCol);
		else
			try {
				getDb().execSQL(sql);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        return cr;
    }
	
	public Cursor getData(String sql,String[] columns,String filter,String type)
    {
		Cursor cr=null;
		if(type=="select")
			cr =getDb().query(sql, columns, filter,null,null,null,null);
		else
			try {
				getDb().beginTransaction();
				String fullSql = sql;
				String subSql="";
				int statementIndex=sql.indexOf(";");
				while(statementIndex>0)
				{
					subSql = fullSql.substring(0,statementIndex);
					getDb().execSQL(subSql);
					fullSql = fullSql.substring(statementIndex+1);
					statementIndex=fullSql.indexOf(";");					
				}
				if (fullSql.length()!=0)
				{
					getDb().execSQL(fullSql);					
				}
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Toast.makeText(context,"10"+e.getMessage(),Toast.LENGTH_LONG).show();
			}
			finally{
				getDb().setTransactionSuccessful();
				getDb().endTransaction();
				
			}
        return cr;
    }

	/**
	 * @param db the db to set
	 */
	public void setDb(SQLiteDatabase db) {
		this.db = db;
	}

	/**
	 * @return the db
	 */
	public SQLiteDatabase getDb() {
		return db;
	}
 
}