package com.android.QuranSteaming;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
	private static MediaPlayer mp = new MediaPlayer();
	PlaylistManager plManger = new PlaylistManager();
	private Updater updater;
    public static QuranSteaming MAIN_ACTIVITY;   
	private final IBinder mBinder = new MyBinder();
	public boolean isRunning = false;
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "Created");
		updater = new Updater();
	}
	@Override
	public void onStart(Intent intent, int startId) {
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
	public void onDestroy() {
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
	    private static final long DELAY = 2000; // one second
	    private boolean isRunning = false;

	    public Updater() {
	    	super("Updater");
	    }

	    @Override
	    public void run() {
	    	while (true) {
	    		if(isRunning){
	    		try {
	    			if(!mp.isPlaying())
	    			{
	    				plManger.isLooping = true;
	    				String path = plManger.getNextItemURL();
	    				if(!path.startsWith("/"))
	    				{
	    					if(!isConnected(MAIN_ACTIVITY.getBaseContext()))
	    					{
	    						QuranSteaming.handler.sendEmptyMessage(QuranSteaming.myHandlerAction.showSorryMessage);
		    					sleep(DELAY);
		    					QuranSteaming.handler.sendEmptyMessage(QuranSteaming.myHandlerAction.dismissDialog);
	    					}
	    				}
	    				if(path!= "")
	    				{
	    					QuranSteaming.handler.sendEmptyMessage(QuranSteaming.myHandlerAction.showWaitMessage);
		    				mp.stop();
		    				mp.reset();
	    					mp.setDataSource(path);
		            		mp.prepare();
		            		mp.start();
		            		QuranSteaming.handler.sendEmptyMessage(QuranSteaming.myHandlerAction.dismissDialog);
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
	public static boolean isConnected(Context context) 
	  {
		  ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		  if (connectivity != null) 
		  {
			  NetworkInfo[] info = connectivity.getAllNetworkInfo();
			  if (info != null) 
				  for (int i = 0; i < info.length; i++) 
					  if (info[i].getState() == NetworkInfo.State.CONNECTED)
					  {
						  return true;
					  }

		  }
		  return false;
	  }
	public void continuePlaying()
	{
		plManger.setPlayingIndex(plManger.getPlayingIndex()-2);
		mp.stop();		
	}
	public void stopPlaying()
	{
		isRunning = false;
	}
	
}
