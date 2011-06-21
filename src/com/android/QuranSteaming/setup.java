package com.android.QuranSteaming;
import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;

public class setup extends Activity {
	DBAdapter dbaAdabter;
	String[] tableColumns;
	Spinner spnLang;
	Spinner spnSites;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (QuranSteaming.language.equalsIgnoreCase("EN"))
        	setContentView(R.layout.setup);
        else
        	setContentView(R.layout.setup_ar);
        spnLang = (Spinner)findViewById(R.id.spnLang);
        spnSites = (Spinner)findViewById(R.id.spnSites);
        dbaAdabter = new DBAdapter(getBaseContext());
        fillSites();
    }
    private void fillSites()
    {
    	ArrayAdapter<CharSequence> adapter1;
		
		try
		{
			adapter1=ArrayAdapter.createFromResource(this,R.array.Server,android.R.layout.simple_spinner_item);
			tableColumns = new String [adapter1.getCount()];
			for (int i=0;i<adapter1.getCount();i++)
			{
				tableColumns[i]= adapter1.getItem(i).toString();
			}
		}
		catch(Exception ex)
		{
			Toast.makeText(this,"7"+ex.getMessage(),Toast.LENGTH_LONG).show();
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
 	    	cr=dbaAdabter.getData("server", tableColumns, "",QuranSteaming.select,"_id");
 	    	startManagingCursor(cr); 
 	    }
	    catch(Exception ex)
	    {
	    	Toast.makeText(this,"9"+ex.getMessage(),Toast.LENGTH_LONG).show();
	    }
		
		try
		{
			String[] from ;
			if (QuranSteaming.language.equalsIgnoreCase("EN"))
				from = new String[] { "server_name_e"};
			else 
				from = new String[] { "server_name_a"};
			int[] to = new int[] { android.R.id.text1 }; 
			SimpleCursorAdapter sca = new SimpleCursorAdapter(this,android.R.layout.simple_spinner_item, cr, from, to);
			sca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spnSites.setAdapter(sca);
			spnSites.setSelection(0);			 
		}
		catch(Exception ex)
		{
			Toast.makeText(this,"10"+ex.getMessage(),Toast.LENGTH_LONG).show();		
		}
		dbaAdabter.close();  
    	
    }
    protected void onPause ()
	{
		try
		{
			super.onPause();
		}
		catch(Exception ex)
		{
			Toast.makeText(setup.this,ex.getMessage(),Toast.LENGTH_LONG).show();
		}
		saveSettings();
		Toast.makeText(setup.this,"Data Saved",Toast.LENGTH_SHORT).show();
		setResult(RESULT_OK);
        finish();
		
	}
    private void saveSettings()
    {
    	try
		{
    		String value ;
    		if (spnLang.getSelectedItemPosition()==0)
    			value = "AR";
    		else
    			value = "EN";
			DBAdapter dbaAdabter = new DBAdapter(this);
			try 
			{
				dbaAdabter.openDataBase();
			}
			catch(Exception ex)
			{
				Toast.makeText(this,ex.getMessage(),Toast.LENGTH_LONG).show();
			}
			String str="update config set lang='"+value+"'";
			dbaAdabter.getData(str, null, null, QuranSteaming.update);
			value = getServerUrl(spnSites.getSelectedItemId());
			str = "update config set server_name= '"+value+"'";
			dbaAdabter.getData(str, null, null, QuranSteaming.update);
			dbaAdabter.close();
		}
		catch(Exception ex)
		{
			Toast.makeText(setup.this,"4"+ex.getMessage(),Toast.LENGTH_LONG).show();
		}
    }
    private String getServerUrl(long serverId)
    {
    	Cursor cr=null;
		String strServerURL="";
		ArrayAdapter<CharSequence> adapter1=ArrayAdapter.createFromResource(this,R.array.Server,android.R.layout.simple_spinner_item);
		tableColumns = new String [adapter1.getCount()];
		String strTableColumns ="";
		for (int i=0;i<adapter1.getCount();i++)
		{
			tableColumns[i]= adapter1.getItem(i).toString();
			strTableColumns = strTableColumns+adapter1.getItem(i).toString()+",";
		}
 	    try{
 	    	cr=dbaAdabter.getData("server", tableColumns, "_id='"+serverId+"'",QuranSteaming.select);
 	    	startManagingCursor(cr);
 	    	cr.moveToFirst();
 			Integer ind=cr.getColumnIndex("url");
 			strServerURL = cr.getString(ind);
 	    }
	    catch(Exception ex)
	    {
	    	Toast.makeText(this,"25"+ex.getMessage(),Toast.LENGTH_LONG).show();
	    }
		return strServerURL;
    }

}
