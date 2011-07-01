package com.android.QuranSteaming;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.RemoteViews;

/**
 * Simulates a download and updates the notification bar with a Progress
 * 
 * @author Nico Heid
 * 
 */
public class DownloadProgress extends Activity {

    public static int totalSize =0 ;
    public static ArrayList<Integer> downloaded;
    private int totalDownloadedSize = 0;
	ProgressBar progressBar;
    private int progress = 0;
    public static boolean isInUse = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // get the layout
        setContentView(R.layout.download_progress);
        // configure the intent
        Intent intent = new Intent(this, DownloadProgress.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

        // configure the notification
        final Notification notification = new Notification(R.drawable.icon, QuranSteaming.language.equalsIgnoreCase("EN")? getApplication().getString(R.string.downloadText):ArabicUtilities.reshape(getApplication().getString(R.string.downloadText_ar)), System.currentTimeMillis());
        notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT;
        notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.download_progress);
        notification.contentIntent = pendingIntent;
        //notification.contentView.setImageViewResource(R.id.status_icon, R.drawable.ic_menu_save);
        notification.contentView.setTextViewText(R.id.status_text, QuranSteaming.language.equalsIgnoreCase("EN")? getApplication().getString(R.string.downloadText):ArabicUtilities.reshape(getApplication().getString(R.string.downloadText_ar)));
        notification.contentView.setProgressBar(R.id.status_progress, 100, progress, false);

        @SuppressWarnings("static-access")
		final NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(
                getApplicationContext().NOTIFICATION_SERVICE);

        notificationManager.notify(12, notification);

        // simulate progress
        Thread download = new Thread() {

            @Override
            public void run() {

                while(getTotalDowbloaded()<totalSize) {
                    progress=totalDownloadedSize*100/totalSize;
                    notification.contentView.setProgressBar(R.id.status_progress, 100, progress, false);

                    // inform the progress bar of updates in progress
                    notificationManager.notify(12, notification);

                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // remove the notification (we're done)
                isInUse = false;
                downloaded.clear();
                notificationManager.cancel(12);                
                totalSize =0;
                onDownloadComplete();
            }
        };
        download.run();
        finish();
    }
    private int getTotalDowbloaded()
    {
    	int totalDownloaded=0;
    	if(downloaded!= null)
	    	for(int i=downloaded.size()-1;i>=0;i--)
	    	{
	    		totalDownloaded += downloaded.get(i);
	    		downloaded.remove(i);
	    	}
    	totalDownloadedSize = totalDownloaded;
    	downloaded.add(totalDownloaded);
		return totalDownloaded;    	
    }
	private void onDownloadComplete() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nm = (NotificationManager) getSystemService(ns);

		long when = System.currentTimeMillis();
		Notification notification = new Notification(R.drawable.icon,
				"Download Completed", when);
		notification.defaults |= Notification.DEFAULT_SOUND
				| Notification.FLAG_AUTO_CANCEL;

		Context context = getApplicationContext();
		CharSequence contentTitle = getTitle().toString();
		Resources r=context.getResources();
		CharSequence contentText = ArabicUtilities.reshape(r.getString(QuranSteaming.language.equalsIgnoreCase("EN")?R.string.downloadCompleted:R.string.downloadCompleted_ar));
		Intent notificationIntent = new Intent(context, QuranSteaming.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		nm.notify(1,notification);
	}

}