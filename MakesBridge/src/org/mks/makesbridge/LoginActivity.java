package org.mks.makesbridge;

import org.json.JSONException;
import org.json.JSONObject;
import org.mks.makesbridge.analytics.GoogleAnalyticsApp;
import org.mks.makesbridge.logger.CustomLogger;

import com.crittercism.app.Crittercism;
import com.crittercism.app.CrittercismConfig;
import com.google.android.gms.analytics.HitBuilders;
import com.logentries.android.AndroidLogger;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import org.mks.makesbridge.analytics.GoogleAnalyticsApp.TrackerName;


public class LoginActivity extends Activity implements OnClickListener {

	ImageView ivLogin;
	EditText etUser, etPass;
	private String bmsToken;
	ConnectionDetector detector;
	AndroidLogger logger;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Google Analytics Configuration
        Tracker t = ((GoogleAnalyticsApp) getApplication()).getTracker(TrackerName.APP_TRACKER);
        t.enableAutoActivityTracking(true);
		t.setScreenName("Login");
		t.send(new HitBuilders.AppViewBuilder().build());
        
        CustomLogger.Initialize(getApplicationContext());
        
        //Create the CrittercismConfig instance.
	    CrittercismConfig config = new CrittercismConfig();
	    boolean shouldCollectLogcat = true;
	    
	    // Enable logcat collection.
	    config.setLogcatReportingEnabled(shouldCollectLogcat);
	    
	    // Initialize.
		Crittercism.initialize(getApplicationContext(), "5693bbbccb99e10e00c7ec93", config);
        
        SharedPreferences pref = getSharedPreferences("mks", 0);
        String bms = pref.getString("bmsToken", "");
        if(bms.equals("") || bms.equals(null))
        {
        	detector = new ConnectionDetector(LoginActivity.this);
            setupWidgets();
        }else{
        	startReportActivity();
        }
        
    }

    private void setupWidgets() {
		try{
			
			etUser = (EditText) findViewById(R.id.etUsername);
			etPass = (EditText) findViewById(R.id.etPassword);
			
			ivLogin = (ImageView) findViewById(R.id.ivLogin);
			ivLogin.setOnClickListener(this);
		}
		catch(Exception ex)
		{
			//logger.error("LoginActivity - setupWidgets: " + ex.getMessage());
			CustomLogger.LogError("LoginActivity - setupWidgets: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void onClick(View v) {
		try{
			switch(v.getId())
			{
			case R.id.ivLogin:
				//Do something when login image is clicked
				if(detector.isConnectingToInternet())
				{
					new AsyncLogin().execute();
				}
				else{
					AlertDialogManager amd = new AlertDialogManager();
					amd.showAlertDialog(LoginActivity.this, "Connection error", "Cannot connect to internet.");
				}
				break;
			}
		}
		catch(Exception ex)
		{
			CustomLogger.LogError("LoginActivity - onClick: " + ex.getMessage());
			//logger.error("LoginActivity - onClick: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	private JSONObject sendLoginDetail()
	{
		JSONObject response = null;
		try{
			WebApiCaller apiInit = new WebApiCaller(LoginActivity.this);
			response = apiInit.SignIn(etUser.getText().toString(), etPass.getText().toString());
		}
		catch(Exception ex)
		{
			CustomLogger.LogError("LoginActivity - sendLoginDetail: " + ex.getMessage());
			//logger.error("LoginActivity - sendLoginDetail: " + ex.getMessage());
			ex.printStackTrace();
		}
		return response;
	}
	
	private void startReportActivity()
	{
		try{
			startActivity(new Intent(LoginActivity.this, ReportsListActivity.class));
			this.finish();
		}
		catch(Exception ex)
		{
			CustomLogger.LogError("LoginActivity - startReportActivity: " + ex.getMessage());
			//logger.error("LoginActivity - startReportActivity: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	private class AsyncLogin extends AsyncTask<Void, Void, JSONObject>
	{

		ProgressDialog dialog = new ProgressDialog(LoginActivity.this);
		
		@Override
		protected void onPreExecute() {
			dialog.setTitle("Communicating");
			dialog.setMessage("Please wait...");
			dialog.setCancelable(false);
			dialog.show();
			super.onPreExecute();
		}
		
		@Override
		protected JSONObject doInBackground(Void... params) {
			JSONObject object=null;
			try{
				object = sendLoginDetail();	
			}
			catch(Exception ex)
			{
				CustomLogger.LogError("LoginActivity - doInBackground: " + ex.getMessage());
				//logger.error("LoginActivity - doInBackground: " + ex.getMessage());
				ex.printStackTrace();
			}
			return object;
		}
		
		@Override
		protected void onPostExecute(JSONObject result) {
			if(dialog.isShowing())
			{
				dialog.dismiss();
				AlertDialogManager amd = new AlertDialogManager();
				if(result.has("errorDetail"))
				{
					try {
						amd.showAlertDialog(LoginActivity.this, "Error", result.getString("errorDetail"));
					} catch (JSONException ex) {
						// TODO Auto-generated catch block
						CustomLogger.LogError("LoginActivity - onPostExecute: " + ex.getMessage());
						//logger.error("LoginActivity - onPostExecute: " + ex.getMessage());
						ex.printStackTrace();
					}
				}
				else{
					try{
						bmsToken = result.getString("bmsToken");
						
						SharedPreferences pref = getSharedPreferences("mks", 0);
						Editor ed = pref.edit();
						ed.putString("bmsToken", bmsToken);
						ed.commit();
						
						Log.d("bmsToken",bmsToken);
						
						//Initiate second activity with list of reports.
						//startActivity(new Intent(LoginActivity.this, ReportsListActivity.class));
						startReportActivity();
					}
					catch(Exception ex)
					{
						CustomLogger.LogError("LoginActivity - onPostExecute: " + ex.getMessage());
						//logger.error("LoginActivity - onPostExecute: " + ex.getMessage());
						ex.printStackTrace();
					}
				}
			}
			super.onPostExecute(result);
		}
		
	}
	
}
