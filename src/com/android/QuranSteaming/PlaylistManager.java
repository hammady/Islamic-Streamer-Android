package com.android.QuranSteaming;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;


public class PlaylistManager {
	private final DBAdapter dbaAdabter= new DBAdapter(QuranSteaming.context);
	private static int playingIndex =0;
	private static String static_url="";
	boolean isLooping= true;
	public int getPlayingIndex(int index) {
		if(index==0)
			return playingIndex;
		else
		{
			int prevIndex=playingIndex;
			Cursor cr1;
			if(index>0)
			{
				cr1=dbaAdabter.getDb().rawQuery("select min(a._id) id from playlist_item a where a._id>"+playingIndex,null);
				if((cr1.getCount()==0)&&(isLooping))
					return 0;
			}
			else
			{
				cr1=dbaAdabter.getDb().rawQuery("select max(a._id) id from playlist_item a where a._id<"+playingIndex,null);
				if((cr1.getCount()==0)&&(isLooping))
					cr1=dbaAdabter.getDb().rawQuery("select max(a._id) id from playlist_item a",null);
				else
					return prevIndex;
				cr1.moveToFirst();
				prevIndex= cr1.getInt(cr1.getColumnIndex("id"));
				cr1=dbaAdabter.getDb().rawQuery("select max(a._id) id from playlist_item a where a._id<"+prevIndex,null);
				if((cr1.getCount()==0))
					prevIndex = -1;
			}
			cr1.moveToFirst();
			prevIndex= cr1.getInt(cr1.getColumnIndex("id"));
		}		
		return index;
	}
	public void setPlayingIndex(int playingIndex) {
		PlaylistManager.playingIndex = playingIndex;
	}
	public PlaylistManager() {
		try 
		{
			dbaAdabter.createDataBase();
		} 
		catch (IOException ioe) 
		{
			throw new Error("Unable to create database");
		}
		try 
		{
			dbaAdabter.openDataBase();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		Cursor cr1=dbaAdabter.getDb().rawQuery("select lang,server_name from config",null);
		cr1.moveToFirst();
		static_url= cr1.getString(cr1.getColumnIndex("server_name"));
		dbaAdabter.close();
	}
	public String getNextItemURL()
	{
		String retURL="";
		try 
		{
			dbaAdabter.openDataBase();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		Cursor cr1=dbaAdabter.getDb().rawQuery("select a._id id,b.url||c.url URL from playlist_item a,shaikh b, surah c, shaikh_surah d where a.shaikh_surah_id = d._id and d.shaikh_id = b._id and d.surah_id = c._id and a._id>"+playingIndex+" limit 1",null);
		if(!cr1.moveToFirst()&&isLooping)
		{
			cr1=dbaAdabter.getDb().rawQuery("select a._id id,b.url||c.url URL from playlist_item a,shaikh b, surah c, shaikh_surah d where a.shaikh_surah_id = d._id and d.shaikh_id = b._id and d.surah_id = c._id and a._id>0 limit 1",null);
			playingIndex = 0;
		}
		if(cr1.moveToFirst())
		{
			playingIndex = Integer.parseInt(cr1.getString(cr1.getColumnIndex("id")));
			retURL = cr1.getString(cr1.getColumnIndex("URL"));
			File f;
			try {
				f = new File(sanitizePath(QuranSteaming.context, "/QuranStreaming/"+retURL));
				if(f.exists())
				{
					return f.getPath();
				}
				retURL = static_url+ retURL;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		dbaAdabter.close();
		return retURL;
	}
	private String sanitizePath(Context con,String path) throws Exception {
		//Toast.makeText(con, "sanitizePath", Toast.LENGTH_LONG).show();
	    if (!path.startsWith("/")) {
	      path = "/" + path;
	    }
	    if (!path.contains(".")) {
	      path += ".mp3";
	    }
	    
	    if(DirectoryNotExists(con,Environment.getExternalStorageDirectory().getAbsolutePath()+new File(path).getParentFile().toString()))
	    {
	    	createFolder();
	    }
	    return Environment.getExternalStorageDirectory().getAbsolutePath() + path;
	}
	private boolean DirectoryNotExists(Context con, String path) throws Exception
	{
		  File directory = new File(path);
		  if (!directory.exists() && !directory.mkdirs()) {
		     
			  //Toast.makeText(con, "error creating folder", Toast.LENGTH_LONG).show();
			  //Log.e(con,"callRecorder","error creating folder");
			  return true;
		  }
		  return false;  
	}
	private void createFolder()
	{
		
	}
}
