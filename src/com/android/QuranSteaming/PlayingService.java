package com.android.QuranSteaming;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * @author Moamen
 *
 */
public class PlayingService extends Service{
	private static final String TAG = PlayingService.class.getSimpleName();
	private boolean isPlaying = false;
	private static final MediaPlayer mp = new MediaPlayer();
	PlaylistManager plManger = new PlaylistManager();
	private Updater updater;
    public static QuranSteaming MAIN_ACTIVITY;   
	private final IBinder mBinder = new MyBinder();
	public boolean isRunning = false;
	@Override
	public synchronized void onCreate() {
		super.onCreate();
		Log.d(TAG, "Created");
		updater = new Updater();
	}
	@Override
	public synchronized void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.d(TAG, "Started");
		if (!updater.isRunning()) {
		      updater.start();
		    }
	}
	public static void setMainActivity(QuranSteaming activity)
    {
      MAIN_ACTIVITY = activity;     
    }
	@Override
	public synchronized void onDestroy() {
		super.onDestroy();
		if(mp!= null)
		{
			mp.stop();
			mp.release();
		}
		if (updater.isRunning()) {
		      updater.interrupt();
		    }
		    updater = null;
		Log.d(TAG, "Destroyed");
	}
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	// ///// Updater Thread
	class Updater extends Thread {
	    private static final long DELAY = 1000; // one second
	    private boolean isRunning = false;

	    public Updater() {
	    	super("Updater");
	    }

	    @Override
	    public void run() {
	    	isRunning = true;
	    	while (isRunning) {
	    		try {
	    			if(!mp.isPlaying())
	    			{
	    				String path = plManger.getNextItemURL();
	    				if(path!= "")
	    				{
		    				mp.setDataSource(path);
		            		mp.prepare();
		            		mp.start();
	    				}
	    			}
	    			Thread.sleep(DELAY);
	    		}
	    		catch (Exception e) {
	    			e.printStackTrace();
	    			isRunning = false;
	    		}
	    	}
	    }
	    public boolean isRunning() {
	        return this.isRunning;
	    }	    
	}
	public boolean getIsPlaying() {
        return isPlaying;
    }
	public void setIsPlaying(boolean playing) {
        isPlaying = playing;
    }
	public class MyBinder extends Binder {
		PlayingService getService() {
			return PlayingService.this;
		}
	}
	public void moveToNext()
	{
		mp.stop();
	}
	public void moveToPrev()
	{
		plManger.setPlayingIndex(plManger.getPlayingIndex()-2);
		mp.stop();		
	}
	public void continuePlaying()
	{
		plManger.setPlayingIndex(plManger.getPlayingIndex()-2);
		mp.stop();		
	}
	
}
