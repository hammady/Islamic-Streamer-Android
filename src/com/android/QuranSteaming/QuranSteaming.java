package com.android.QuranSteaming;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class QuranSteaming extends Activity  {
	static enum playMode{
		playlist,
		singleItem,
		empty
	}
	static class HandlerAction{
		final int HideMenuBar = 10;
		public int getHideMenuBar() {
			return HideMenuBar;
		}
		public int getDismissDialog() {
			return dismissDialog;
		}
		final int dismissDialog = 0;
		final int notifyListViewAdapter = 1;
		final int showSorryMessage = 2;
		final int showWaitMessage= 3;
	};
	static HandlerAction myHandlerAction= new HandlerAction();
	public static boolean isRunning = false;
	PlaylistManager plManger;
	public static DBAdapter dbaAdabter;
	String[] tableColumns;
	public static String select ="select";
	public static String update ="update";
	public static String static_url;
	public static String static_path;
	public static String language ="";
	public static MediaPlayer mp;
	public static Context context;
	public static playMode playingMode = playMode.empty;
	public static boolean isPaused = false;
	static PhoneStateListener listener;
	Spinner spnCategory;
	Spinner spnShaikh;
	static Integer selectedCount = 0;
	static ProgressDialog progDailog;
	static boolean Mediaready= false;
	Long selectedCategory;
	static Long  selectedShaikh;
	static Long  playingShaikh;
	static String selectedShaikhURL;
	static Long selectedSurah;
	static String sourceFile;
	static View alaMenuLayout;
	static String selectedSurahURL;
	static boolean isPausedDueToCall = false;
	//private Updater playlistUpdater=new Updater();
	/**
	 * buttons definition	
	 */
	ImageButton btnStartStream;
	ImageButton btnDownload;
	ImageButton stopStreamButton;
	ImageButton btnMoveToNext;
	ImageButton btnMoveToPrev;
	ImageButton btnAddToPlaylist;
	/**
	 * end buttons definition
	 */
	static ListView lvSurah;	
	ArrayList<ProjectObject> Surahs=null;
	ListAdapter arrayAdapter;
	private PlayingService s;
	private static boolean waitTillPlay=false;

	public static Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			final Resources r=context.getResources();
			if(msg.what==myHandlerAction.getHideMenuBar())
			{
				if(selectedCount>0)
				{
					alaMenuLayout.findViewById(R.id.btnStartStream).setVisibility(View.VISIBLE);
					alaMenuLayout.findViewById(R.id.btnStopStream).setVisibility(View.INVISIBLE);
				}
				else
				{
					alaMenuLayout.setVisibility(View.INVISIBLE);
					alaMenuLayout.findViewById(R.id.btnStartStream).setVisibility(View.INVISIBLE);
					alaMenuLayout.findViewById(R.id.btnStopStream).setVisibility(View.INVISIBLE);
					alaMenuLayout.findViewById(R.id.btnPlayNext).setVisibility(View.INVISIBLE);
					alaMenuLayout.findViewById(R.id.btnPlayPrevious).setVisibility(View.INVISIBLE);
				}
			}
			else if (msg.what==myHandlerAction.getDismissDialog()) 
			{
				progDailog.dismiss();
			}
			else if (msg.what==myHandlerAction.notifyListViewAdapter)
			{
				((BaseAdapter)lvSurah.getAdapter()).notifyDataSetChanged();
			}
			else if(msg.what==myHandlerAction.showSorryMessage)
			{
				if(language.equalsIgnoreCase("EN"))
	        		progDailog = ProgressDialog.show(context,r.getString(R.string.Sorry), r.getString(R.string.noInternetConnection),true);
	        	else
	        		progDailog = ProgressDialog.show(context,ArabicUtilities.reshape(r.getString(R.string.Sorry_ar)), ArabicUtilities.reshape(r.getString(R.string.noInternetConnection_ar)),true);
			}
			else if(msg.what==myHandlerAction.showWaitMessage)
			{
				if(language.equalsIgnoreCase("EN"))
	        		progDailog = ProgressDialog.show(context,r.getString(R.string.txtStreamingText), r.getString(R.string.txtStreamingWaitText),true);
	        	else
	        		progDailog = ProgressDialog.show(context,ArabicUtilities.reshape(r.getString(R.string.txtStreamingText_ar)), ArabicUtilities.reshape(r.getString(R.string.txtStreamingWaitText_ar)),true);
			}
		}
		};
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        selectedCount=0;
        PlayingService.setMainActivity(this);
        //creating an intent for the service        
	    Intent intent = new Intent(this,PlayingService.class);
	    ComponentName x= startService(intent); 
	    bindService(intent, mConnection, 0);
		//doBindService();
		//showHideMenu();
        dbaAdabter = new DBAdapter(getBaseContext());
        //checking db existance
		try 
		{
			dbaAdabter.createDataBase();
		} 
		catch (IOException ioe) 
		{
			throw new Error("Unable to create database");
		}
		//opening database
		try 
		{
			dbaAdabter.openDataBase();
		}
		catch(Exception ex)
		{
			Toast.makeText(this,"6"+ex.getMessage(),Toast.LENGTH_LONG).show();
		}
		Cursor cr1=dbaAdabter.getDb().rawQuery("select lang,server_name from config",null);
		cr1.moveToFirst();
		Integer ind=cr1.getColumnIndex("lang");
		language = cr1.getString(ind);
		static_path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/QuranStreaming/";
		
		try
		{
			/*if (language.equalsIgnoreCase("EN"))
				setContentView(R.layout.main);
			else*/ 
				setContentView(R.layout.main_ar);
		}
		catch (Exception e) {
			Toast.makeText(this,"6"+e.getMessage(),Toast.LENGTH_LONG).show();
		}
		ind=cr1.getColumnIndex("server_name");
		static_url= cr1.getString(ind);
		cr1.close();
		dbaAdabter.close();
				
		if((language.length()==0) || (language.equals( null)))
		{
			showSetupForm();
		}
		context = this;
		alaMenuLayout = (View)findViewById(R.id.alaMenuLayout);
		lvSurah = (ListView) findViewById(R.id.lvSurah);
		lvSurah.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				isRunning=false;
				final ImageView ivImg =(ImageView)arg0.findViewById(R.id.ivImg);
				View menuView = ((View)((View)((View)((View)((View)arg0.getParent()).getParent()).getParent()).getParent()).getParent()).findViewById(R.id.alaMenuLayout);
				if(menuView.findViewById(R.id.btnStopStream).getVisibility()==View.VISIBLE)
				{
					ivImg.setVisibility(View.INVISIBLE);
					QuranSteaming.stopStreamingAudio(arg0);
					return;
				}
				//v2.setVisibility(View.VISIBLE);
				//v2.findViewById(R.id.btnStopstream).setVisibility(View.VISIBLE);
				TextView tvSurahId =(TextView)arg0.findViewById(R.id.tvSurahId);
				QuranSteaming.selectedSurah = Long.parseLong(tvSurahId.getText().toString());
				QuranSteaming.selectedSurahURL=getSurahURL(QuranSteaming.selectedSurah);
				playingMode= playMode.singleItem;
				Listen(ivImg,arg0);
			}
		});
		// buttons on click listeners
		buttonsSetOnClickLister();
		//spinner on item selected listener
		spinnerSetOnitemSelectLister();
		
		fillCategoy(); 
		addTelephoneListener();
		isMediaPlayingThread();
    }
    private void addTelephoneListener()
    {
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		// Create a new PhoneStateListener
	    listener = new PhoneStateListener() {
	      @Override
	      public void onCallStateChanged(int state, String incomingNumber) {
	        try{
	        	switch (state) {
		        case TelephonyManager.CALL_STATE_IDLE:
		        	
		        	if(isPausedDueToCall)
		        	{
		        		isPausedDueToCall = false;
		        		if(playingMode == playMode.singleItem)
		        		{
		        			mp.start();
		        			findViewById(R.id.btnStartStream).setVisibility(View.INVISIBLE);
		        			findViewById(R.id.btnStopStream).setVisibility(View.VISIBLE);
		        			findViewById(R.id.alaMenuLayout).setVisibility(View.VISIBLE);
		        		}
		        		else
		        		{
		        			if(s!=null)
		        				s.continuePlaying();
		        		}
		        	}
		          break;
		        case TelephonyManager.CALL_STATE_OFFHOOK:
		        	if(mp.isPlaying())
		        	{
			        	isPausedDueToCall = true;
			        	stopStreamingAudio(findViewById(R.id.alaMenuLayout));
		        	}
		          break;
		        case TelephonyManager.CALL_STATE_RINGING:
		        	if(mp.isPlaying())
		        	{
			        	isPausedDueToCall = true;
			        	stopStreamingAudio(findViewById(R.id.alaMenuLayout));
		        	}
		          break;
		        }
	        }
	        catch(Exception e)
	        {
	        	e.printStackTrace();
	        }
	      }
	    };
		tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
    }
    private void Listen(ImageView v,View v2)
    {
    	QuranSteaming.startStreamingAudio(v,QuranSteaming.selectedSurahURL,v2);
    }
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		try{
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.mainmenu, menu);
			return true;
		}
		catch(Exception ex)
		{
			Toast.makeText(this,"11"+ex.getMessage(),Toast.LENGTH_LONG).show();
			return true;
		}
	}
 // This method is called once the menu is selected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			// We have only one menu option
			case R.id.iSetting:
				// Launch Preference activity
				Intent i = new Intent(QuranSteaming.this, setup.class);
				startActivity(i);
				break;
			case R.id.iQuit:
				quitApplication();
				break;
			default:
				break;
		}
		return true;
	}
    void showSetupForm()
	{
		try
		{
			Intent i = new Intent(QuranSteaming.this, setup.class);
			startActivity(i);
		}
		catch(Exception ex)
		{
			Toast.makeText(QuranSteaming.this,ex.getMessage(),Toast.LENGTH_LONG).show();
		}
		
	}
	private void fillCategoy()
	{
		ArrayAdapter<CharSequence> adapter1;
		
		try
		{
			adapter1=ArrayAdapter.createFromResource(this,R.array.Category,android.R.layout.simple_spinner_item);
			tableColumns = new String [adapter1.getCount()];
			for (int i=0;i<adapter1.getCount();i++)
			{
				tableColumns[i]= adapter1.getItem(i).toString();
			}
		}
		catch(Exception ex)
		{
			Toast.makeText(QuranSteaming.this,"7"+ex.getMessage(),Toast.LENGTH_LONG).show();
		}
		
		try 
		{
			dbaAdabter.openDataBase();
		}
		catch(Exception ex)
		{
			Toast.makeText(this,"8"+ex.getMessage(),Toast.LENGTH_LONG).show();
		}
		Cursor cr=null;
		
 	    try
 	    {
 	    	cr=dbaAdabter.getData("category", tableColumns, "",select,"_id");
 	    	startManagingCursor(cr); 
 	    }
	    catch(Exception ex)
	    {
	    	Toast.makeText(this,"9"+ex.getMessage(),Toast.LENGTH_LONG).show();
	    }
		
		try
		{
			String[] from ;
			if (language.equalsIgnoreCase("EN"))
				from = new String[] { "category_name_e"};
			else 
				from = new String[] { "category_name_a"};
			int[] to = new int[] { android.R.id.text1 }; 
			SimpleCursorAdapter sca = new SimpleCursorAdapter(this,android.R.layout.simple_spinner_item, cr, from, to);
			sca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spnCategory.setAdapter(sca);
			spnShaikh.setSelection(0);
			selectedCategory=spnCategory.getSelectedItemId();
			 
		}
		catch(Exception ex)
		{
			Toast.makeText(this,"10"+ex.getMessage(),Toast.LENGTH_LONG).show();		
		}
		dbaAdabter.close();
		fillShaikh(spnCategory.getSelectedItemId()>0?spnCategory.getSelectedItemId():2 , "1");
	}
	public void fillShaikh(long categoryId,String selectMode)
	{
		ArrayAdapter<CharSequence> adapter1;
		String strTableColumns ="";
		try{
			adapter1=ArrayAdapter.createFromResource(this,R.array.shaikh,android.R.layout.simple_spinner_item);
			tableColumns = new String [adapter1.getCount()];
			for (int i=0;i<adapter1.getCount();i++)
			{
				tableColumns[i]= adapter1.getItem(i).toString();
				strTableColumns = strTableColumns+adapter1.getItem(i).toString()+",";
			}
		}
		catch(Exception ex)
		{
			Toast.makeText(QuranSteaming.this,"12"+ex.getMessage(),Toast.LENGTH_LONG).show();
		}
		
		Cursor cr=null;
		
 	    try{
 	    	String sortKey;
			if (language.equalsIgnoreCase("EN"))
				sortKey = "shaikh_name_e";
			else 
				sortKey = "shaikh_name_a";
 	    	cr=dbaAdabter.getData("shaikh", tableColumns, "category_id='"+categoryId+"'",select,sortKey);
 	    	startManagingCursor(cr);
 	    	cr.moveToFirst();
 	    }
	    catch(Exception ex)
	    {
	    	Toast.makeText(this,"13"+ex.getMessage(),Toast.LENGTH_LONG).show();
	    }
		
		try{
			
			String[] from;
			if (language.equalsIgnoreCase("EN"))
				from = new String[] { "shaikh_name_e"};
			else 
				from = new String[] { "shaikh_name_a"};
			int[] to = new int[] { android.R.id.text1 }; 
			SimpleCursorAdapter sca = new SimpleCursorAdapter(this,android.R.layout.simple_spinner_item, cr, from, to);
			sca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spnShaikh.setAdapter(sca);
			spnShaikh.setSelection(0);
			selectedShaikh=spnShaikh.getSelectedItemId();
			selectedShaikhURL = getShaikhURL(selectedShaikh);			
		}catch(Exception ex)
		{
			Toast.makeText(this,"14"+ex.getMessage(),Toast.LENGTH_LONG).show();		
		}
		dbaAdabter.close();
		fillSurah(selectedShaikh);
		
	}
	public String getShaikhURL(Long shaikhID)
	{
		
		Cursor cr=null;
		String strShaikhURL="";
		ArrayAdapter<CharSequence> adapter1=ArrayAdapter.createFromResource(this,R.array.shaikh,android.R.layout.simple_spinner_item);
		tableColumns = new String [adapter1.getCount()];
		String strTableColumns ="";
		for (int i=0;i<adapter1.getCount();i++)
		{
			tableColumns[i]= adapter1.getItem(i).toString();
			strTableColumns = strTableColumns+adapter1.getItem(i).toString()+",";
		}
 	    try{
 	    	cr=dbaAdabter.getData("shaikh", tableColumns, "_id='"+shaikhID+"'",select,language!="EN"?"shaikh_name_e":"shaikh_name_a");
 	    	startManagingCursor(cr);
 	    	cr.moveToFirst();
 			Integer ind=cr.getColumnIndex("url");
 			strShaikhURL = cr.getString(ind);
 	    }
	    catch(Exception ex)
	    {
	    	Toast.makeText(this,"25"+ex.getMessage(),Toast.LENGTH_LONG).show();
	    }
	    dbaAdabter.close();
		return strShaikhURL;
	}
	public static  void startStreamingAudio(ImageView v,String surahURL,View v2) {
		waitTillPlay = true;
    	Mediaready=false;
    	final Resources r=context.getResources();
        boolean isConn = isConnected(context);
        File surahPath = new File(static_path+selectedShaikhURL+surahURL);
        sourceFile=static_url+selectedShaikhURL+selectedSurahURL ;
        if(surahPath.exists())
        {
        	sourceFile = static_path+selectedShaikhURL+surahURL;
        }
        if(isConn||surahPath.exists())
    	{
    		if(language.equalsIgnoreCase("EN"))
        		progDailog = ProgressDialog.show(context,r.getString(R.string.txtStreamingText), r.getString(R.string.txtStreamingWaitText),true);
        	else
        		progDailog = ProgressDialog.show(context,ArabicUtilities.reshape(r.getString(R.string.txtStreamingText_ar)), ArabicUtilities.reshape(r.getString(R.string.txtStreamingWaitText_ar)),true);
    		v2.setVisibility(View.VISIBLE);
    		v2.findViewById(R.id.btnStopStream).setVisibility(View.VISIBLE);
    		if (selectedCount==0)
    			v2.findViewById(R.id.btnStartStream).setVisibility(View.INVISIBLE);
    		if(playingMode==playMode.playlist)
    		{
    			v2.findViewById(R.id.btnPlayNext).setVisibility(View.VISIBLE);
        		v2.findViewById(R.id.btnPlayPrevious).setVisibility(View.VISIBLE);
    		}
    		
    	}
    	else
    	{
    		if(language.equalsIgnoreCase("EN"))
        		progDailog = ProgressDialog.show(context,r.getString(R.string.Sorry), r.getString(R.string.noInternetConnection),true);
        	else
        		progDailog = ProgressDialog.show(context,ArabicUtilities.reshape(r.getString(R.string.Sorry_ar)), ArabicUtilities.reshape(r.getString(R.string.noInternetConnection_ar)),true);
    	}
		new Thread() {
            @Override
            public void run() {
            	File surahPath = new File(sourceFile);
            	
            	if(!isConnected(context)&&!surahPath.exists())
            	{
            		try {
						sleep(4000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
            		handler.sendEmptyMessage(myHandlerAction.getDismissDialog());
            		waitTillPlay= false;
            		return;
            	}
            	try { 
            		if (mp ==null )
            			mp = new MediaPlayer();
            		else
            		{
            			mp.stop();
            			mp.release();
            			mp = new MediaPlayer();
            		}
            		//pb1.setVisibility(View.VISIBLE);
            		
            		mp.setDataSource(sourceFile);
            		mp.prepare();
            		mp.start();
            		Mediaready = true;
            		handler.sendEmptyMessage(myHandlerAction.getDismissDialog());
            		waitTillPlay= false;
            	} catch (IOException e) {
            		handler.sendEmptyMessage(myHandlerAction.getDismissDialog());
            		waitTillPlay= false;
            	} catch (Exception e) {
					e.printStackTrace();
					handler.sendEmptyMessage(myHandlerAction.getDismissDialog());
					waitTillPlay= false;
				}        
            }
        }.start();
    }
	/**
	   * Checks if the phone has network connection.
	   * 
	   * @param context the context
	   * @return <code>true</code> if the phone is connected
	   */
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
	public static void stopStreamingAudio(View v)
	{
		if ((mp != null))
		{
			
			v.findViewById(R.id.btnStopStream).setVisibility(View.INVISIBLE);
			v.findViewById(R.id.btnStartStream).setVisibility(View.VISIBLE);
			if(mp.isPlaying())
			{
				mp.pause();
				isPaused = true;
			}
		}
	}
	public void onDestroy(Bundle savedInstanceState) {
		mp.stop();
		mp.release();
	}
	public static void setLanguage(String lang) {
		QuranSteaming.language = lang;
	}
	public static String getLanguage() {
		return QuranSteaming.language;
	}
	public static void setSelectedSurah(Long surah) {
		QuranSteaming.selectedSurah = surah;
	}
	public static Long getSelectedSurah() {
		return QuranSteaming.selectedSurah;
	}
	public static void setSelectedShaikh(Long shaikh) {
		QuranSteaming.selectedSurah = shaikh;
	}
	public static Long getSelectedShaikh() {
		return QuranSteaming.selectedShaikh;
	}
	public void fillSurah(long ShaikhId)
	{
		Surahs = new ArrayList<ProjectObject>();
        if (language.equalsIgnoreCase("EN"))
        {
        	arrayAdapter = new ListAdapter(QuranSteaming.this, R.layout.listview,Surahs);
        }
        else
        {
        	arrayAdapter = new ListAdapter(QuranSteaming.this, R.layout.listview_ar,Surahs);
        }
        lvSurah.setAdapter(arrayAdapter);
        lvSurah.setItemsCanFocus(false);
        lvSurah.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        
       /***
        * normal DB code
        */
        ArrayAdapter<CharSequence> adapter1;
		String strTableColumns ="";
		//lvSurah= getListView();
		lvSurah.setCacheColorHint(0);

		try{
			adapter1=ArrayAdapter.createFromResource(this,R.array.surah,android.R.layout.simple_list_item_1);
			tableColumns = new String [adapter1.getCount()];
			for (int i=0;i<adapter1.getCount();i++)
			{
				tableColumns[i]= adapter1.getItem(i).toString();
				strTableColumns = strTableColumns+adapter1.getItem(i).toString()+",";
			}
		}
		catch(Exception ex)
		{
			Toast.makeText(QuranSteaming.this,"12"+ex.getMessage(),Toast.LENGTH_LONG).show();
		}
		
		Cursor cr=null;
		
		try 
		{
			dbaAdabter.openDataBase();
		}
		catch(Exception ex)
		{
			Toast.makeText(this,"6"+ex.getMessage(),Toast.LENGTH_LONG).show();
		}
 	    try{
 	    	cr=dbaAdabter.getDb().rawQuery("select distinct "+strTableColumns.substring(0, strTableColumns.length()-1)+" from surah a cross join shaikh_surah b on a._id = b.surah_id and b.shaikh_id ='"+ShaikhId+"' left outer join playlist_item c on b._id = c.shaikh_surah_id order by a._id",null);
 	    	startManagingCursor(cr);
 	    }
	    catch(Exception ex)
	    {
	    	Toast.makeText(this,"13"+ex.getMessage(),Toast.LENGTH_LONG).show();
	    }
	    /***
	     * end normal DB code
	     */
	    if(cr.getCount()>0)
	    {
	    	String colName="";
	    	if (QuranSteaming.language.equalsIgnoreCase("EN"))
    			colName ="surah_name_e";
			else 
				colName ="surah_name_a";
	    	int nameInd =cr.getColumnIndex(colName);
	    	int idInd =cr.getColumnIndex("_id");
	    	//int idIsChecked =cr.getColumnIndex("is_checked");
	    	int idIsPlaying =cr.getColumnIndex("is_playing");
	    	if(cr.moveToFirst())
	    	{
		    	do
		    	{
		    		ProjectObject o=new ProjectObject();
		    		o.surahName= ArabicUtilities.reshape(cr.getString(nameInd));
			    	o.surahId= Integer.parseInt(cr.getString(idInd));
			    	if((cr.getString(idIsPlaying) != null)&&(cr.getString(idIsPlaying).length() != 0))
			    		o.isPlaying = true;
			    	//if((cr.getString(idIsChecked) != null)&&(cr.getString(idIsChecked).length() != 0))
			    		o.isChecked = false;
		    		Surahs.add(o);
		    	}while(cr.moveToNext());
	    	}
	    	arrayAdapter.notifyDataSetChanged();
	    }
	    dbaAdabter.close();
	}
	public static String getShaikhSurahID(String ShaikhId,String SurahId)
	{
		String ShaikhSurahId="";
		
		Cursor cr=null;
		ArrayAdapter<CharSequence> adapter1=ArrayAdapter.createFromResource(QuranSteaming.context,R.array.shaikhSurah,android.R.layout.two_line_list_item);
		String[] tableColumns = new String [adapter1.getCount()];
		String strTableColumns ="";
		DBAdapter dbaAdabter= new DBAdapter(QuranSteaming.context);
		try 
		{
			dbaAdabter.openDataBase();
		}
		catch(Exception ex)
		{
			//Toast.makeText(this,"6"+ex.getMessage(),Toast.LENGTH_LONG).show();
		}
		for (int i=0;i<adapter1.getCount();i++)
		{
			tableColumns[i]= adapter1.getItem(i).toString();
			strTableColumns = strTableColumns+adapter1.getItem(i).toString()+",";
		}
		try{
 	    	cr=dbaAdabter.getData("shaikh_surah", tableColumns, "surah_id='"+SurahId+"' and shaikh_id='"+ShaikhId+"'","select");
 	    	/*if (selectMode=="1")
 	    		cr=dbaAdabter.getData("shaikh", tableColumns, "category_id='"+categoryId+"'",select,"shaikh_name_e");
 	    	else
 	    	{
 	    		cr=dbaAdabter.getDb().rawQuery("select "+strTableColumns.substring(0, strTableColumns.length()-1)+" from shaikh where category_id='"+categoryId+"' order by shaikh_name_e",null);
 	    	}*/
 	    	//startManagingCursor(cr);
 	    	cr.moveToFirst();
 			Integer ind=cr.getColumnIndex("_id");
 			ShaikhSurahId = cr.getString(ind);
 	    }
	    catch(Exception ex)
	    {
	    	//Toast.makeText(this,"23"+ex.getMessage(),Toast.LENGTH_LONG).show();
	    }
	    dbaAdabter.close();
		
		return ShaikhSurahId;
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
	private void createFolder()
	{
		  
	}
	private void download(final android.widget.ListAdapter listAdapter)
	{
		if(!isConnected(context))
    	{
    		if(language.equalsIgnoreCase("EN"))
        		progDailog = ProgressDialog.show(context,getApplication().getString(R.string.Sorry), getApplication().getString(R.string.noInternetConnection),true);
        	else
        		progDailog = ProgressDialog.show(context,ArabicUtilities.reshape(getApplication().getString(R.string.Sorry_ar)), ArabicUtilities.reshape(getApplication().getString(R.string.noInternetConnection_ar)),true);
    		new Thread(){
				@Override
	            public void run() {
            	try{
            		sleep(4000);
            	}
            	catch (Exception e) {
            		e.printStackTrace();
				}
            	handler.sendEmptyMessage(myHandlerAction.getDismissDialog());
        		return;
			};
    		}.start();
    		return;
    	}
		DownloadProgress.totalSize = 0;
		if(isConnected(context))
		{ 
			if(language.equalsIgnoreCase("EN"))
        		progDailog = ProgressDialog.show(context,getApplication().getString(R.string.preparingDownload), getApplication().getString(R.string.preparingDownloadDetails),true);
        	else
        		progDailog = ProgressDialog.show(context,ArabicUtilities.reshape(getApplication().getString(R.string.preparingDownload_ar)), ArabicUtilities.reshape(getApplication().getString(R.string.preparingDownloadDetails_ar)),true);
			new Thread(){
				@Override
	            public void run() {
					try {
						for(int i=0;i<listAdapter.getCount();i++)
			        	{
							final ProjectObject po=(ProjectObject) listAdapter.getItem(i);
							QuranSteaming.selectedSurah = (long) po.surahId;
							selectedSurahURL=getSurahURL(selectedSurah);
							if(po.isChecked)
							{
								try {
									DownloadProgress.totalSize += getFileSize(static_url+selectedShaikhURL+selectedSurahURL,sanitizePath(context, "/QuranStreaming/"+selectedShaikhURL+"/"+selectedSurahURL));
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
							if(DownloadProgress.downloaded==null) 
								DownloadProgress.downloaded= new ArrayList<Integer>();
			        	}
						handler.sendEmptyMessage(myHandlerAction.dismissDialog);
						if(DownloadProgress.totalSize!=0)
						{
				    		if(!DownloadProgress.isInUse)
							{
								try
								{
									Intent intent = new Intent(QuranSteaming.this,DownloadProgress.class);
									DownloadProgress.isInUse= true;
									startActivity(intent);
								}
								catch (Exception e) {
									e.printStackTrace();
								}
							} 
				    		for(int i=0;i<listAdapter.getCount();i++)
				        	{
								final ProjectObject po=(ProjectObject) listAdapter.getItem(i);
								QuranSteaming.selectedSurah = (long) po.surahId;
								selectedSurahURL=getSurahURL(selectedSurah);
								
								if(po.isChecked)
								{
									try {
										new Thread(){
											@Override
								            public void run() {
												try {
													DownloadFromUrl(static_url+selectedShaikhURL+selectedSurahURL,static_path+selectedShaikhURL+selectedSurahURL,po.surahName);
												} catch (Exception e) {
													e.printStackTrace();
												}
											};
										}.start();
									}
									catch (Exception e) {
										e.printStackTrace();
									}
									po.isChecked = false;
								}
				        	}
						}
						else
						{
							for(int i=0;i<listAdapter.getCount();i++)
				        	{
								final ProjectObject po=(ProjectObject) listAdapter.getItem(i);
								if(po.isChecked)
								{
									po.isChecked = false;
								}
				        	}
						}
			    		handler.sendEmptyMessage(myHandlerAction.notifyListViewAdapter);
			    		selectedCount=0;
			    		((View)((View)lvSurah.getParent()).getParent().getParent().getParent().getParent()).findViewById(R.id.btnDownload).setVisibility(View.INVISIBLE);
			    		((View)((View)lvSurah.getParent()).getParent().getParent().getParent().getParent()).findViewById(R.id.btnAddToPlaylist).setVisibility(View.INVISIBLE);
			    		if(((View)((View)lvSurah.getParent()).getParent().getParent().getParent().getParent()).findViewById(R.id.btnStopStream).getVisibility()==View.INVISIBLE)
			    		{
			    			((View)((View)lvSurah.getParent()).getParent().getParent().getParent().getParent()).findViewById(R.id.alaMenuLayout).setVisibility(View.INVISIBLE);
			    		}
					} catch (Exception e) {
						e.printStackTrace();
					}
				};
			}.start();
		}
	}
	private int getFileSize(String strURL,String filepath)
	{
		int size=0;
		File f = new File(filepath);
		if(f.exists())
			return 0;
		try {	
		    URL url = new URL( strURL); //you can write here any link
            /* Open a connection to that URL. */
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            size = c.getContentLength();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return size;
	}
	public void DownloadFromUrl(String strURL, String fileName,String surahName) {  //this is the downloader method
		File f = new File(fileName);
		if(f.exists())
			return;
		
        /*DownloadManager d=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);;
        if(language.equalsIgnoreCase("EN"))
        	d.enqueue(new Request(srcUri).setAllowedNetworkTypes(Request.NETWORK_MOBILE|Request.NETWORK_WIFI)
        		.setAllowedOverRoaming(false).setTitle(context.getString(R.string.btnDownload_ar)+surahName).setDestinationUri(destUri));
        else
        	d.enqueue(new Request(srcUri).setAllowedNetworkTypes(Request.NETWORK_MOBILE|Request.NETWORK_WIFI)
            		.setAllowedOverRoaming(false).setTitle(context.getString(R.string.btnDownload_ar) +surahName).setDestinationUri(destUri));
		*/

		try {
			//WebView wvWebView= (WebView) findViewById(R.id.wvWebView);
			
		    URL url = new URL( strURL); //you can write here any link

            long startTime = System.currentTimeMillis();
            Log.d("Muslim Bag", "download begining");
            Log.d("Muslim Bag", "download url:" + url);
            Log.d("Muslim Bag", "downloaded file name:" + fileName);
            /* Open a connection to that URL. */
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setDoOutput(true);
            c.connect();
            /*
             * Define InputStreams to read from the URLConnection.
             */
            InputStream is = c.getInputStream();
            FileOutputStream f1 = new FileOutputStream(f);
            /*
             * Read bytes to the Buffer until there is nothing more to read(-1).
             */
            byte[] buffer = new byte[50000];
            int len1 = 0;
            while ( (len1 = is.read(buffer)) > 0 ) {
                f1.write(buffer,0, len1);
                DownloadProgress.downloaded.add(len1);
            }

            f1.close();

            /* Convert the Bytes read to a String. */
            Log.d("Muslim Bag", "download ready in"
                            + ((System.currentTimeMillis() - startTime) / 1000)
                            + " sec");

        } catch (IOException e) {
                Log.d("Muslim Bag", "Error: " + e);
        }
	}
	public String getSurahURL(Long surahID)
	{
		Cursor cr=null;
		String strSurahURL="";
		ArrayAdapter<CharSequence> adapter1=ArrayAdapter.createFromResource(QuranSteaming.context,R.array.surah_url,android.R.layout.simple_spinner_item);
		String[] tableColumns = new String [adapter1.getCount()];
		String strTableColumns ="";
		DBAdapter dbaAdabter= new DBAdapter(QuranSteaming.context);
		try 
		{
			dbaAdabter.openDataBase();
		}
		catch(Exception ex)
		{
			//Toast.makeText(this,"6"+ex.getMessage(),Toast.LENGTH_LONG).show();
		}
		for (int i=0;i<adapter1.getCount();i++)
		{
			tableColumns[i]= adapter1.getItem(i).toString();
			strTableColumns = strTableColumns+adapter1.getItem(i).toString()+",";
		}
		try{
 	    	cr=dbaAdabter.getData("surah", tableColumns, "_id='"+surahID+"'","select");
 	    	cr.moveToFirst();
 			Integer ind=cr.getColumnIndex("url");
 			strSurahURL = cr.getString(ind);
 	    }
	    catch(Exception ex)
	    {
	    	//Toast.makeText(this,"23"+ex.getMessage(),Toast.LENGTH_LONG).show();
	    }
	    dbaAdabter.close();
		return strSurahURL;
	}
	private void onPlayClick(View v)
	{
		if((selectedCount<1)&&(playingMode==playMode.singleItem))
		{
			playingMode= playMode.singleItem;
			mp.start();
			v.findViewById(R.id.btnStartStream).setVisibility(View.INVISIBLE);
			v.findViewById(R.id.btnPlayNext).setVisibility(View.INVISIBLE);
			v.findViewById(R.id.btnPlayPrevious).setVisibility(View.INVISIBLE);
			v.findViewById(R.id.btnStopStream).setVisibility(View.VISIBLE);
			return;
		}
		else if(selectedCount==1)
		{
			playingMode=playMode.singleItem;
			v.findViewById(R.id.btnStartStream).setVisibility(View.INVISIBLE);
			v.findViewById(R.id.btnStopStream).setVisibility(View.VISIBLE);
			v.findViewById(R.id.btnPlayNext).setVisibility(View.INVISIBLE);
			v.findViewById(R.id.btnPlayPrevious).setVisibility(View.INVISIBLE);
		}
		else
		{
			playingMode=playMode.playlist;
			v.findViewById(R.id.btnStartStream).setVisibility(View.INVISIBLE);
			v.findViewById(R.id.btnStopStream).setVisibility(View.VISIBLE);
			v.findViewById(R.id.btnPlayNext).setVisibility(View.VISIBLE);
			v.findViewById(R.id.btnPlayPrevious).setVisibility(View.VISIBLE);
		}
		isRunning = true;
		
		//playlistUpdater.start();
		startService();
	}
	private void onPauseClick(View v)
	{
		if(playingMode==playMode.singleItem)
		{
			stopStreamingAudio((View) v.getParent());
			isRunning= false;
		}
		else
		{
			
		}
	}
	private void moveToNext(View view)
	{
		if(s!=null)
			s.moveToNext();
	}
	private void moveToPrev(View view)
	{
		if(s!=null)
			s.moveToPrev();
	}
	private void spinnerSetOnitemSelectLister()
	{
		spnCategory = (Spinner)findViewById(R.id.spnCategory);
		spnCategory.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				long newSelectedValue=arg3;
				
				if(selectedCategory!=newSelectedValue)
				{
					if(((View)arg1.getParent().getParent().getParent().getParent().getParent()).findViewById(R.id.alaMenuLayout).findViewById(R.id.btnStopStream).getVisibility()==View.INVISIBLE)
						((View)arg1.getParent().getParent().getParent().getParent().getParent()).findViewById(R.id.alaMenuLayout).setVisibility(View.INVISIBLE);
					((View)arg1.getParent().getParent().getParent().getParent().getParent()).findViewById(R.id.alaMenuLayout).findViewById(R.id.btnDownload).setVisibility(View.INVISIBLE);
					fillShaikh(newSelectedValue, "1");
					selectedCategory = newSelectedValue;
				}			
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
		
		});
		spnShaikh = (Spinner) findViewById(R.id.spnShaikh);
		spnShaikh.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				long newSelectedValue=arg3;
				
				if(selectedShaikh!=newSelectedValue)
				{
					selectedCount  = 0;
					if(((View)arg1.getParent().getParent().getParent().getParent().getParent()).findViewById(R.id.alaMenuLayout).findViewById(R.id.btnStopStream).getVisibility()==View.INVISIBLE)
						((View)arg1.getParent().getParent().getParent().getParent().getParent()).findViewById(R.id.alaMenuLayout).setVisibility(View.INVISIBLE);
					((View)arg1.getParent().getParent().getParent().getParent().getParent()).findViewById(R.id.alaMenuLayout).findViewById(R.id.btnDownload).setVisibility(View.INVISIBLE);
					selectedShaikh = newSelectedValue;
					selectedShaikhURL = getShaikhURL(selectedShaikh);
					fillSurah(selectedShaikh);
				}			
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
			
			
		
		});
	}
	private void buttonsSetOnClickLister()
	{
		btnStartStream = (ImageButton) findViewById(R.id.btnStartStream);
		btnStartStream.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				onPlayClick((View) view.getParent());
			}});
		
		btnMoveToNext = (ImageButton) findViewById(R.id.btnPlayNext);
		btnMoveToNext.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				moveToNext(view);
			}});
		
		btnMoveToPrev = (ImageButton) findViewById(R.id.btnPlayPrevious);
		btnMoveToPrev.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				moveToPrev(view);
			}});
		
		stopStreamButton = (ImageButton) findViewById(R.id.btnStopStream);
		stopStreamButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				View v = (View)view.getParent(); 
				stopStreamingAudio(v);
				onPauseClick(view);
	       }});
		
		btnDownload = (ImageButton) findViewById(R.id.btnDownload);
		btnDownload.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				download(lvSurah.getAdapter());
			}});
		
		btnAddToPlaylist = (ImageButton) findViewById(R.id.btnAddToPlaylist);
		btnAddToPlaylist.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//download(lvSurah.getAdapter());
			}});
	}
	private void showHideMenu()
	{
		if((s!=null)&&(s.getIsPlaying()))
		{
			findViewById(R.id.alaMenuLayout).setVisibility(View.VISIBLE);
			findViewById(R.id.alaMenuLayout).findViewById(R.id.btnPlayNext).setVisibility(View.VISIBLE);
			findViewById(R.id.alaMenuLayout).findViewById(R.id.btnPlayPrevious).setVisibility(View.VISIBLE);
			//findViewById(R.id.alaMenuLayout).findViewById(R.id.btnStartStream).setVisibility(View.VISIBLE);
			findViewById(R.id.alaMenuLayout).findViewById(R.id.btnStopStream).setVisibility(View.VISIBLE);			
		}
	}
	@Override
	protected void onDestroy() {
	  super.onDestroy();
	 
	  try {
	    //api.removeListener(collectorListener);
	    //unbindService(serviceConnection);
	  } catch (Throwable t) {
	    // catch any issues, typical for destroy routines
	    // even if we failed to destroy something, we need to continue destroying
	    
	  }
	 
	  
	}
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			s = ((PlayingService.MyBinder) binder).getService();
			Toast.makeText(QuranSteaming.this, "Connected",
					Toast.LENGTH_SHORT).show();
		}
		@Override
		public void onServiceDisconnected(ComponentName className) {
			s = null;
		}
	};
	void doBindService() {
		bindService(new Intent(this, PlayingService.class), mConnection,
				Context.BIND_AUTO_CREATE);
	}
	private void quitApplication()
	{
		finish();
	}
	public playMode getPlayingMode() {
		return playingMode;
	}
	public static void setPlayingMode(playMode playingMode) {
		QuranSteaming.playingMode = playingMode;
	}
	private void isMediaPlayingThread()
	{
		new Thread(){
			@Override
            public void run() {
        	try{
        		while(true)
        		{
        			sleep(1000);
        			if((mp!=null)&&(!mp.isPlaying())&&(alaMenuLayout.getVisibility()==View.VISIBLE))
        			{
        				sleep(6000);
        				if((!waitTillPlay)&&(!mp.isPlaying())&&(playingMode!=playMode.playlist))
        				{
        					handler.sendEmptyMessage(myHandlerAction.getHideMenuBar());
        				}
        			}
        		}
        	}
        	catch (Exception e) {
        		e.printStackTrace();
			}
		};
		}.start();
	}
	class Updater extends Thread {
	    private static final long DELAY = 3000; // three second

	    public Updater() {
	    	super("Updater");
	    }

	    @Override
	    public void run() {
	    	while (isRunning) {
	    		try {
	    			if(mp==null)
	    			{
	    				mp = new MediaPlayer();
	    			}
	    			if(!mp.isPlaying())
	    			{
	    				plManger.isLooping = true;
	    				String path = plManger.getNextItemURL();
	    				if(!path.startsWith("/"))
	    				{
	    					if(!isConnected(context))
	    					{
		    					handler.sendEmptyMessage(myHandlerAction.showSorryMessage);
		    					sleep(DELAY);
		    					handler.sendEmptyMessage(myHandlerAction.dismissDialog);
	    					}
	    				}
	    				if(path!= "")
	    				{
	    					handler.sendEmptyMessage(myHandlerAction.showWaitMessage);
		    				mp.stop();
		    				mp.reset();
	    					mp.setDataSource(path);
		            		mp.prepare();
		            		mp.start();
		            		handler.sendEmptyMessage(myHandlerAction.dismissDialog);
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
	private void startService(){
    	Intent intent = new Intent(this, PlayingService.class);
    	//intent.putExtra(QuranDataService.DWONLOAD_TYPE_KEY, PlayingService.DOWNLOAD_QURAN_IMAGES);
    	try{
	    	if ((s==null) ||(!s.isRunning))
	    		startService(intent);
	    	boolean b=getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	    	if(b)
	    	{
	    		return;
	    	}
    	}
    	catch (Exception e) {
			e.printStackTrace();
		}
    }
	private boolean isMyServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if ("com.android.QuranSteaming.PlayingService".equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
}