package org.mks.makesbridge;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mks.makesbridge.analytics.GoogleAnalyticsApp;
import org.mks.makesbridge.analytics.GoogleAnalyticsApp.TrackerName;
import org.mks.makesbridge.logger.CustomLogger;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.logentries.android.AndroidLogger;
import com.nhaarman.listviewanimations.appearance.simple.SwingBottomInAnimationAdapter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ReportsListActivity extends Activity implements OnItemClickListener {

	ListView lsReports;
	List<String> reportName;
	List<String> createDate;
	private int OffSet = 0;
	int Totalcount=0;
	int count = 10;
	boolean isFirst = true;
	ListRow adapter;
	boolean isUpdate = false;
	SwingBottomInAnimationAdapter animationAdapter;
	ConnectionDetector detector;
	AndroidLogger logger;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reportlist);
		
		Tracker t = ((GoogleAnalyticsApp) getApplication()).getTracker(TrackerName.APP_TRACKER);
		t.setScreenName("ReportList");
		t.send(new HitBuilders.AppViewBuilder().build());
		
		detector = new ConnectionDetector(ReportsListActivity.this);
		setUpWidgets();
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		if(detector.isConnectingToInternet())
		{
			if(Totalcount == 0)
			{
				new AsyncReports().execute();
			}
		}
		else{
			AlertDialogManager amd = new AlertDialogManager();
			amd.showAlertDialog(ReportsListActivity.this, "Connection error", "Cannot connect to internet.");
		}
		super.onResume();
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		GoogleAnalytics.getInstance(ReportsListActivity.this).reportActivityStop(this);
	}
	
	private void setUpWidgets() {
		try{
			lsReports = (ListView) findViewById(R.id.lsReports);
			lsReports.setOnItemClickListener(this);
			lsReports.setOnScrollListener(new OnScrollListener() {
			    @Override
			    public void onScrollStateChanged(AbsListView view, int scrollState) {
			    }

			    @Override
			    public void onScroll(AbsListView view, int firstVisibleItem,
			                int visibleItemCount, int totalItemCount) {
			       int lastItem = lsReports.getLastVisiblePosition()+1;
			       Log.d("Total Visible Items", String.valueOf(lastItem));
			       if(lastItem == totalItemCount && lastItem != Totalcount) {
			    	   if(lastItem%20==0 && lastItem < Totalcount && Totalcount != 0)
			    	   {
			    		   OffSet += 20;
			    		   new AsyncReports().execute();
			    	   }
			       }
			    }
			});
			reportName = new ArrayList<String>();
			createDate = new ArrayList<String>();
		}
		catch(Exception ex)
		{
			CustomLogger.LogError("ReportListActivity - setUpWidgets: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub
	}
	
	private void updateView()
	{
		try{
			adapter = new ListRow(ReportsListActivity.this);
			animationAdapter = new SwingBottomInAnimationAdapter(adapter);
			animationAdapter.setAbsListView(lsReports);
			lsReports.setAdapter(animationAdapter);
			isUpdate = true;
		}
		catch(Exception ex)
		{
			CustomLogger.LogError("ReportListActivity - updateView: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	//Custom list Adapter for Report list class.
	private class ListRow extends ArrayAdapter<String> {
		private final Context context;

		public ListRow(Context context) {
			super(context, R.layout.reportlist_item, reportName);
			this.context = context;
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {

			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View rowView = inflater.inflate(R.layout.reportlist_item, parent, false);
			try {

				TextView tvReportName = (TextView) rowView
						.findViewById(R.id.tvReportName);
				TextView tvDate = (TextView) rowView.findViewById(R.id.tvCreationDate);
				
				tvReportName.setText(reportName.get(position));
				tvDate.setText("created on " + createDate.get(position));
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return rowView;
		}
	}
	
	private void GetReports()
	{
		SharedPreferences pref = getSharedPreferences("mks", 0);
		try{
			WebApiCaller caller = new WebApiCaller(ReportsListActivity.this);
			
			String bms = pref.getString("bmsToken", "");
			Log.d("InitiateGetReports", bms);
			JSONObject obj = caller.GetReports(bms, OffSet);
			
			Totalcount = Integer.parseInt(obj.getString("totalCount"));
			int count = Integer.parseInt(obj.getString("count"));
			JSONArray _reports = obj.getJSONArray("reports");
			JSONObject object = _reports.getJSONObject(0);
			int index = 0;
			for(int i=0; i<count; i++)
			{
				if(object != null){
					index=i+1;
					JSONArray subArray = object.getJSONArray("report"+index);
					for(int j=0; j<subArray.length(); j++)
					{
						reportName.add(subArray.getJSONObject(j).getString("reportName"));
						createDate.add(subArray.getJSONObject(j).getString("creationDate"));
					}
				}
			}
		}
		catch(Exception ex)
		{
			CustomLogger.LogError("ReportListActivity - GetReports: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	//Async Call for webapi execution.
	private class AsyncReports extends AsyncTask<Void, Void, Void>
	{

		ProgressDialog dialog = new ProgressDialog(ReportsListActivity.this);
		
		@Override
		protected void onPreExecute() {
			dialog.setCancelable(false);
			dialog.setTitle("Fetching Information");
			dialog.setMessage("Please wait...");
			dialog.show();
			super.onPreExecute();
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			try{
				GetReports();
			}
			catch(Exception ex)
			{
				isUpdate = true;
				ex.printStackTrace();
				CustomLogger.LogError("ReportListActivity - doInBackground: " + ex.getMessage());
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(dialog.isShowing())
			{
				dialog.dismiss();
				if(!isUpdate)
				{
					updateView();
				}else{
					animationAdapter.notifyDataSetChanged();
				}
			}
			super.onPostExecute(result);
		}
		
	}
	
}
