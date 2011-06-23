package com.android.QuranSteaming;



import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
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
    public static int totalDownloaded = 0;
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
        notification.contentView.setImageViewResource(R.id.status_icon, R.drawable.ic_menu_save);
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

                while(totalDownloaded<totalSize) {
                    progress=totalDownloaded*100/totalSize;
                    notification.contentView.setProgressBar(R.id.status_progress, 100, progress, false);

                    // inform the progress bar of updates in progress
                    notificationManager.notify(12, notification);

                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                // remove the notification (we're done)
                notificationManager.cancel(12);
                totalDownloaded = 0;
                totalSize =0;
                isInUse = false;
                
            }
        };

        download.run();

        finish();

    }

}