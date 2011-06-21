package com.android.QuranSteaming;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;


public class QuranSteaming extends Activity  {
	public static DBAdapter dbaAdabter;
	String[] tableColumns;
	public static String select ="select";
	public static String update ="update";
	public static String static_url;
	public static String static_path;
	public static String language ="";
	public static MediaPlayer mp;
	public static Context context;
	int screenWidth;
	int screenHeight;
	Spinner spnCategory;
	Spinner spnShaikh;
	static Integer selectedCount = 0;
	static ProgressDialog progDailog;
	static Dialog dailog;
	static boolean Mediaready= false;
	Long selectedCategory;
	static Long  selectedShaikh;
	static Long  playingShaikh;
	static String selectedShaikhURL;
	static Long selectedSurah;
	static String sourceFile;
	Button streamButton;
	Button btnDownload;
	ListView lvSurah;
	Button stopStreamButton;
	ImageButton playButton;
	ArrayList<ProjectObject> Surahs=null;
	ListAdapter arrayAdapter;
	static String selectedSurahURL;
	private static Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

		}
		};
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        selectedCount=0;
        
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
		getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		try
		{
			if (language.equalsIgnoreCase("EN"))
				setContentView(R.layout.main);
			else 
				setContentView(R.layout.main_ar);
		}
		catch (Exception e) {
			// TODO: handle exception
			Toast.makeText(this,"6"+e.getMessage(),Toast.LENGTH_LONG).show();
		}
		ind=cr1.getColumnIndex("server_name");
		static_url= cr1.getString(ind);
		cr1.close();
		dbaAdabter.close();
		getScreenSize();
		
		if((language.length()==0) || (language.equals( null)))
		{
			showSetupForm();
		}
		context = this;
		//textStreamed = (TextView)findViewById(R.id.tvtextStreamed);
		lvSurah = (ListView) findViewById(R.id.lvSurah);
		lvSurah.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				return;
				
			}
		});
		
		stopStreamButton = (Button) findViewById(R.id.btnStopstream);
		
		stopStreamButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				View v = (View)view.getParent(); 
				stopStreamingAudio(v);
        }});
		//TableRow tr = (TableRow)findViewById(R.id.tbLowerButton);
		//tr.setVisibility(View.VISIBLE); 
		spnShaikh = (Spinner) findViewById(R.id.spnShaikh);
		//spnShaikh.setLayoutParams(new LayoutParams((int) (screenWidth*.6), LayoutParams.WRAP_CONTENT));
		int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(50,
				View.MeasureSpec.AT_MOST);
				        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0,
				View.MeasureSpec.UNSPECIFIED);
		spnShaikh.measure(widthMeasureSpec, heightMeasureSpec);
		btnDownload = (Button) findViewById(R.id.btnDownload);
		btnDownload.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				download(lvSurah.getAdapter());
			}
		});
		spnCategory = (Spinner)findViewById(R.id.spnCategory);
		//spnCategory.setLayoutParams(new LayoutParams((int) (screenWidth*.4), LayoutParams.WRAP_CONTENT));
		spnCategory.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				long newSelectedValue=arg3;
				
				if(selectedCategory!=newSelectedValue)
				{
					if(((View)arg1.getParent().getParent().getParent().getParent().getParent()).findViewById(R.id.alaMenuLayout).findViewById(R.id.btnStopstream).getVisibility()==View.INVISIBLE)
						((View)arg1.getParent().getParent().getParent().getParent().getParent()).findViewById(R.id.alaMenuLayout).setVisibility(View.INVISIBLE);
					((View)arg1.getParent().getParent().getParent().getParent().getParent()).findViewById(R.id.alaMenuLayout).findViewById(R.id.btnDownload).setVisibility(View.INVISIBLE);
					fillShaikh(newSelectedValue, "1");
					selectedCategory = newSelectedValue;
				}			
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
		
		});
		
		spnShaikh.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				long newSelectedValue=arg3;
				
				if(selectedShaikh!=newSelectedValue)
				{
					selectedCount  = 0;
					if(((View)arg1.getParent().getParent().getParent().getParent().getParent()).findViewById(R.id.alaMenuLayout).findViewById(R.id.btnStopstream).getVisibility()==View.INVISIBLE)
						((View)arg1.getParent().getParent().getParent().getParent().getParent()).findViewById(R.id.alaMenuLayout).setVisibility(View.INVISIBLE);
					((View)arg1.getParent().getParent().getParent().getParent().getParent()).findViewById(R.id.alaMenuLayout).findViewById(R.id.btnDownload).setVisibility(View.INVISIBLE);
					selectedShaikh = newSelectedValue;
					selectedShaikhURL = getShaikhURL(selectedShaikh);
					fillSurah(selectedShaikh);
				}			
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
			
			
		
		});				
		fillCategoy(); 
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
		
    	Mediaready=false;
    	final Resources r=context.getResources();
        boolean isConn = isConnected(context);
        File surahPath = new File(static_path+selectedShaikhURL+surahURL);
        sourceFile=static_url+selectedShaikhURL+selectedSurahURL ;
        if(surahPath.exists())
        {
        	sourceFile = static_path+selectedShaikhURL+surahURL;
        }
        //if()
        if(isConn||surahPath.exists())
    	{
    		if(language.equalsIgnoreCase("EN"))
        		progDailog = ProgressDialog.show(context,r.getString(R.string.txtStreamingText), r.getString(R.string.txtStreamingWaitText),true);
        	else
        		progDailog = ProgressDialog.show(context,r.getString(R.string.txtStreamingText_ar), r.getString(R.string.txtStreamingWaitText_ar),true);
    		v2.setVisibility(View.VISIBLE);
    		v2.findViewById(R.id.btnStopstream).setVisibility(View.VISIBLE);
    	}
    	else
    	{
    		if(language.equalsIgnoreCase("EN"))
        		progDailog = ProgressDialog.show(context,r.getString(R.string.Sorry), r.getString(R.string.noInternetConnection),true);
        	else
        		progDailog = ProgressDialog.show(context,r.getString(R.string.Sorry_ar), r.getString(R.string.noInternetConnection_ar),true);
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
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            		handler.sendEmptyMessage(0);
            		progDailog.dismiss();
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
            		handler.sendEmptyMessage(0);
            		progDailog.dismiss();
            		//pb1.setVisibility(View.INVISIBLE);
            	} catch (IOException e) {
            		handler.sendEmptyMessage(0);
            		progDailog.dismiss();
            		        		
            	} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					handler.sendEmptyMessage(0);
            		progDailog.dismiss();
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
		if ((mp != null)&&(mp.isPlaying()))
		{
			mp.stop();
			v.findViewById(R.id.btnStopstream).setVisibility(View.INVISIBLE);
			if(v.findViewById(R.id.btnDownload).getVisibility()==View.INVISIBLE)
			{
				v.setVisibility(View.INVISIBLE);
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
	    	int idIsChecked =cr.getColumnIndex("is_checked");
	    	int idIsPlaying =cr.getColumnIndex("is_playing");
	    	cr.moveToFirst();
	    	ProjectObject o;   	
	    	
	    	while(!cr.isLast())
	    	{
	    		o=new ProjectObject();
	    		o.surahName= cr.getString(nameInd);
		    	o.surahId= Integer.parseInt(cr.getString(idInd));
		    	if((cr.getString(idIsPlaying) != null)&&(cr.getString(idIsPlaying).length() != 0))
		    		o.isPlaying = true;
		    	if((cr.getString(idIsChecked) != null)&&(cr.getString(idIsChecked).length() != 0))
		    		o.isChecked = true;
	    		Surahs.add(o);	    		
	    		cr.moveToNext();
	    	}
	    	o=new ProjectObject();
	    	if((cr.getString(idIsPlaying) != null)&&(cr.getString(idIsPlaying).length() != 0))
	    		o.isPlaying = true;
	    	if((cr.getString(idIsChecked) != null)&&(cr.getString(idIsChecked).length() != 0))
	    		o.isChecked = true;
	    	o.surahName= cr.getString(nameInd);
	    	o.surahId= Integer.parseInt(cr.getString(idInd));;
	    	Surahs.add(o);
	    	arrayAdapter.notifyDataSetChanged();
	    }
	    dbaAdabter.close();
	}
	private void getScreenSize()
	{
		WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE); 
	    Display display = window.getDefaultDisplay();
	    screenWidth = display.getWidth();
	    screenHeight = display.getHeight();
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
        		progDailog = ProgressDialog.show(context,getApplication().getString(R.string.Sorry_ar), getApplication().getString(R.string.noInternetConnection_ar),true);
    		new Thread(){
				@Override
	            public void run() {
            	try{
            		sleep(4000);
            	}
            	catch (Exception e) {
					// TODO: handle exception
				}
            	handler.sendEmptyMessage(0);
        		progDailog.dismiss();
        		return;
			};
    		}.start();
    		return;
    	}
		
		if(isConnected(context))
		{ 
			new Thread(){
				@Override
	            public void run() {
		       	ProjectObject po;
            	try{
	            	for(int i=0;i<listAdapter.getCount();i++)
	            	{
						po=(ProjectObject) listAdapter.getItem(i);
						QuranSteaming.selectedSurah = (long) po.surahId;
						selectedSurahURL=getSurahURL(selectedSurah);
						
						if(po.isChecked)
						{
							try {
								DownloadFromUrl(static_url+selectedShaikhURL+selectedSurahURL,sanitizePath(context, "/QuranStreaming/"+selectedShaikhURL+"/"+selectedSurahURL),po.surahName);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
	            	}
            	} catch(Exception e)
            	{
            		return;
            	}
				};
			}.start();
		}
	}
	public void DownloadFromUrl(String strURL, String fileName,String surahName) {  //this is the downloader method
		Uri srcUri =Uri.parse(strURL);
		File f = new File(fileName);
		if(f.exists())
			return;
		Uri destUri = Uri.fromFile(f);
		
        DownloadManager d=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);;
        if(language.equalsIgnoreCase("EN"))
        	d.enqueue(new Request(srcUri).setAllowedNetworkTypes(Request.NETWORK_MOBILE|Request.NETWORK_WIFI)
        		.setAllowedOverRoaming(false).setTitle(context.getString(R.string.btnDownload_ar)+surahName).setDestinationUri(destUri));
        else
        	d.enqueue(new Request(srcUri).setAllowedNetworkTypes(Request.NETWORK_MOBILE|Request.NETWORK_WIFI)
            		.setAllowedOverRoaming(false).setTitle(context.getString(R.string.btnDownload_ar) +surahName).setDestinationUri(destUri));
		/*setProgressBarIndeterminateVisibility(true);

		try {
			//WebView wvWebView= (WebView) findViewById(R.id.wvWebView);
			
		    URL url = new URL( strURL); //you can write here any link
            File file = new File(fileName);

            long startTime = System.currentTimeMillis();
            Log.d("ImageManager", "download begining");
            Log.d("ImageManager", "download url:" + url);
            Log.d("ImageManager", "downloaded file name:" + fileName);
            /* Open a connection to that URL. */
            /*HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setDoOutput(true);
            c.connect();
            int totalSize = c.getContentLength();
            int downloadedSize = 0;

            //this.requestWindowFeature(Window.FEATURE_PROGRESS);
            
            /*
             * Define InputStreams to read from the URLConnection.
             */
            /*InputStream is = c.getInputStream();
            FileOutputStream f = new FileOutputStream(file);
            /*
             * Read bytes to the Buffer until there is nothing more to read(-1).
             */
            /*byte[] buffer = new byte[10000];
            int len1 = 0;
            while ( (len1 = is.read(buffer)) > 0 ) {
                f.write(buffer,0, len1);
                downloadedSize += buffer.length;
                getWindow().setFeatureInt(Window.FEATURE_PROGRESS, (downloadedSize*1000/totalSize));
            }

            f.close();

            /* Convert the Bytes read to a String. */
            /*Log.d("ImageManager", "download ready in"
                            + ((System.currentTimeMillis() - startTime) / 1000)
                            + " sec");

        } catch (IOException e) {
                Log.d("MP3Manager", "Error: " + e);
        }
        setProgressBarIndeterminateVisibility(false);*/
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
}