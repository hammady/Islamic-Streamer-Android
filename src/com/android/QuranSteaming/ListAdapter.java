package com.android.QuranSteaming;

import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import java.util.ArrayList;

import com.android.QuranSteaming.QuranSteaming.playMode;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class ListAdapter extends ArrayAdapter<ProjectObject> implements
OnClickListener {
private Context context;

final String SETTING_TODOLIST = "todolist";
RelativeLayout alaListItemId;
static boolean isManuallyChecked= false;

public ListAdapter(Context context, int textViewResourceId,
	ArrayList<ProjectObject> dataItems) {
	super(context, textViewResourceId, dataItems);
	this.context = context;
	}
 
	@Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
    	
    	View v = convertView;
        //Get the current alert object
        String surahName = getItem(position).surahName;
        int surahId = getItem(position).surahId;
        
        if (v == null) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			if(QuranSteaming.language.equalsIgnoreCase("EN"))
				v = inflater.inflate(R.layout.listview, null);
			else
				v = inflater.inflate(R.layout.listview_ar, null);
		}
        alaListItemId = (RelativeLayout)v.findViewById(R.id.alaListItemId);
    	if (position%2==0)
    		alaListItemId.setBackgroundColor(Color.GRAY);
    	else
    		alaListItemId.setBackgroundColor(Color.DKGRAY);
        
        /*//Inflate the view
        if(convertView==null)
        {
            alertView = new LinearLayout(getContext());
            String inflater = Context.LAYOUT_INFLATER_SERVICE;
            LayoutInflater vi;
            vi = (LayoutInflater)getContext().getSystemService(inflater);
            vi.inflate(resource, alertView, true);
        }
        else
        {
            alertView = (LinearLayout) convertView;
        }*/
        //Get the text boxes from the listitem.xml file
        TextView tvSurahName =(TextView)v.findViewById(R.id.tvSurahName);
        TextView tvSurahId =(TextView)v.findViewById(R.id.tvSurahId);
        TextView tvItemPosition =(TextView)v.findViewById(R.id.tvItemPosition);
        ImageView ivImg =(ImageView)v.findViewById(R.id.ivImg);
        CheckBox cb = (CheckBox)v.findViewById(R.id.cbListViewItem);
        if(cb.getVisibility()!=View.VISIBLE)
	        if (getItem(position).isChecked)
	        {
	        	QuranSteaming.selectedCount ++;
	        }
        cb.setOnCheckedChangeListener(null);
        cb.setChecked(getItem(position).isChecked);
        cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
			View v = (View)arg0.getParent();
			TextView tv=(TextView) v.findViewById(R.id.tvSurahId);
			TextView tvItemPosition=(TextView) v.findViewById(R.id.tvItemPosition);
			try 
			{
				QuranSteaming.dbaAdabter.openDataBase();
			}
			catch(Exception ex)
			{
				Toast.makeText(context,ex.getMessage(),Toast.LENGTH_LONG).show();
			}
			
			String str;
			String itemValue="";
			itemValue=QuranSteaming.getShaikhSurahID(String.valueOf(QuranSteaming.selectedShaikh),tv.getText().toString());
			Integer position = Integer.parseInt(tvItemPosition.getText().toString());
			if(isChecked)
			{
				if(QuranSteaming.selectedShaikh!= QuranSteaming.playingShaikh)
				{
					QuranSteaming.selectedCount=0;
					str= "delete from playlist_item";
					QuranSteaming.dbaAdabter.getData(str, null, null, QuranSteaming.update);
				}
				QuranSteaming.playingShaikh = QuranSteaming.selectedShaikh;
				str="insert into playlist_item(playlist_id, shaikh_surah_id) values('1','"+itemValue+"')";
				if(!isManuallyChecked)
				{
					ListAdapter.this.getItem(position).isChecked=true;
					QuranSteaming.selectedCount++;
				}
			}
			else
			{
				str="delete from playlist_item where playlist_id='1' and shaikh_surah_id='"+itemValue+"'";
				ListAdapter.this.getItem(position).isChecked=false;
				QuranSteaming.selectedCount--;
			}
			View v2=((View)((View)((View)((View)((View)((View)v.getParent()).getParent()).getParent()).getParent()).getParent().getParent().getParent()).findViewById(R.id.alaMenuLayout));
			if(QuranSteaming.selectedCount==0)
			{
				if((v2.findViewById(R.id.btnStopStream).getVisibility()!=View.VISIBLE)&&(QuranSteaming.isPaused!=true))
				{
					v2.setVisibility(View.INVISIBLE);
					v2.findViewById(R.id.btnStartStream).setVisibility(View.INVISIBLE);
				}
				if((QuranSteaming.mp!=null)&&(QuranSteaming.mp.isPlaying()))
					v2.findViewById(R.id.btnStartStream).setVisibility(View.INVISIBLE);
				v2.findViewById(R.id.btnDownload).setVisibility(View.INVISIBLE);
				v2.findViewById(R.id.btnAddToPlaylist).setVisibility(View.INVISIBLE);
			}
			else
			{
				v2.setVisibility(View.VISIBLE);
				v2.findViewById(R.id.btnDownload).setVisibility(View.VISIBLE);
				v2.findViewById(R.id.btnAddToPlaylist).setVisibility(View.VISIBLE);
				v2.findViewById(R.id.btnStartStream).setVisibility(View.VISIBLE);
			}
			if(!isManuallyChecked)
				QuranSteaming.dbaAdabter.getData(str, null, null, QuranSteaming.update);
			QuranSteaming.dbaAdabter.close();
		}
		});
		ivImg.setOnClickListener(new OnClickListener() {
        	
			@Override
			public void onClick(View v) {
				ImageView ivImg =(ImageView)v;
				ivImg.setVisibility(View.INVISIBLE);
				//QuranSteaming.stopStreamingAudio();
			}
		});
        tvSurahName.setOnClickListener(new OnClickListener() {
        	
			@Override
			public void onClick(View v) {
				QuranSteaming.isRunning=false;
				View nv=(View) v.getParent();
				//hideAllPlayImage((ListView)nv.getParent().getParent());
				final ImageView ivImg =(ImageView)((View)nv.getParent()).findViewById(R.id.ivImg);
				View v2 = ((View)((View)((View)((View)((View)((View)v.getParent()).getParent()).getParent()).getParent()).getParent().getParent().getParent()).findViewById(R.id.alaMenuLayout)); 
				if(v2.findViewById(R.id.btnStopStream).getVisibility()==View.VISIBLE)
				{
					ivImg.setVisibility(View.INVISIBLE);
					QuranSteaming.stopStreamingAudio(v2);
					return;
				}
				//v2.setVisibility(View.VISIBLE);
				//v2.findViewById(R.id.btnStopstream).setVisibility(View.VISIBLE);
				TextView tvSurahId =(TextView)((View) v.getParent()).findViewById(R.id.tvSurahId);
				QuranSteaming.selectedSurah = Long.parseLong(tvSurahId.getText().toString());
				QuranSteaming.selectedSurahURL=getSurahURL(QuranSteaming.selectedSurah);
				QuranSteaming.setPlayingMode(playMode.singleItem);
				Listen(ivImg,v2);
			}
		});
        //Assign the appropriate data from our alert object above
        tvSurahName.setText(surahName);
        tvSurahId.setText(String.valueOf(surahId));
        tvItemPosition.setText(String.valueOf(position));
        if (getItem(position).isChecked)
        {
        	isManuallyChecked = true;
        	//cb.setChecked(true);
        	isManuallyChecked = false;
        }
        return v;
    }
    private void Listen(ImageView v,View v2)
    {
    	QuranSteaming.startStreamingAudio(v,QuranSteaming.selectedSurahURL,v2);
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

	@Override
	public void onClick(View v) {
		View nv=(View) v.getParent().getParent();
		hideAllPlayImage((ListView)nv);
		final ImageView ivImg =(ImageView)((View) v.getParent()).findViewById(R.id.ivImg);
		View v2 = ((View)((View)((View)((View)((View)((View)v.getParent()).getParent()).getParent()).getParent()).getParent().getParent().getParent()).findViewById(R.id.alaMenuLayout));
		if(ivImg.getVisibility()==View.VISIBLE)
		{
			ivImg.setVisibility(View.INVISIBLE);
			QuranSteaming.stopStreamingAudio(v2);
			return;
		}
		//v2.setVisibility(View.VISIBLE);
		//v2.findViewById(R.id.btnStopstream).setVisibility(View.VISIBLE);
		//ivImg.setVisibility(View.VISIBLE);
		TextView tvSurahId =(TextView)((View) v.getParent()).findViewById(R.id.tvSurahId);
		QuranSteaming.selectedSurah = Long.parseLong(tvSurahId.getText().toString());
		QuranSteaming.selectedSurahURL=getSurahURL(QuranSteaming.selectedSurah); 
		Listen(ivImg,v2);
	}
	private void hideAllPlayImage(ListView lv)
	{
		for(int x=0;x<lv.getCount();x++)
		{
			ProjectObject po = (ProjectObject) lv.getItemAtPosition(x);
			po.isPlaying = false;
		}
		((BaseAdapter)lv.getAdapter()).notifyDataSetChanged();
	} 
}
