package org.mks.makesbridge;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mks.sslcertificate.NetworkUtility;

import android.content.Context;
import android.util.Log;

@SuppressWarnings("deprecation")
public class WebApiCaller {

	private Context ctx;
	
	WebApiCaller(Context context)
	{
		this.ctx = context;
	}
	
	public static final String Url = "https://test.bridgemailsystem.com/pms/"; 

	InputStream inputStream = null;

	public JSONObject SignIn(String Username, String Password) {
		JSONObject array = null;
		try {

			@SuppressWarnings("resource")
			HttpClient httpClient = new  org.mks.sslcertificate.MyHttpClient(ctx);
			
			HttpPost postData = new HttpPost("https://test.bridgemailsystem.com/pms/mobile/mobileService/mobileLogin");

			ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
					2);
			nameValuePairs.add(new BasicNameValuePair("userId", Username));
			nameValuePairs.add(new BasicNameValuePair("password", Password));

			postData.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = httpClient.execute(postData);
			HttpEntity entity = response.getEntity();

			inputStream = entity.getContent();

			try {
				BufferedReader bReader = new BufferedReader(
						new InputStreamReader(inputStream, "iso-8859-1"), 8);
				StringBuilder sBuilder = new StringBuilder();

				String line = null;
				while ((line = bReader.readLine()) != null) {
					sBuilder.append(line + "\n");
				}

				inputStream.close();
				
				String result = sBuilder.toString();

				array = new JSONObject(result);

			} catch (Exception e) {
				Log.e("StringBuilding & BufferedReader",
						"Error converting result " + e.toString());
			}
		} catch (Exception ex) {
			Log.e("WebApiCaller - SignIn Error: ", "Error converting result "
					+ ex.getMessage().toString());
		}
		return array;
	}

	public JSONObject GetReports(String bmsToken, int OffSet) {
		JSONObject array = null;
		try {

			@SuppressWarnings("resource")
			HttpClient httpClient = new  org.mks.sslcertificate.MyHttpClient(ctx);
			
			String getUrl = String.format("https://test.bridgemailsystem.com/pms/io/user/customReports/?BMS_REQ_TK=%1$s&offset=%2$s&type=get&bucket=20&isMobile=Y&userId=demo", bmsToken, OffSet);
			
			Log.d("getURl", getUrl);
			
			HttpGet getData = new HttpGet(getUrl);

			HttpResponse response = httpClient.execute(getData);
			HttpEntity entity = response.getEntity();

			inputStream = entity.getContent();

			try {
				BufferedReader bReader = new BufferedReader(
						new InputStreamReader(inputStream, "iso-8859-1"), 8);
				StringBuilder sBuilder = new StringBuilder();

				String line = null;
				while ((line = bReader.readLine()) != null) {
					sBuilder.append(line + "\n");
				}

				inputStream.close();
				
				String result = sBuilder.toString();

				array = new JSONObject(result);

			} catch (Exception e) {
				Log.e("StringBuilding & BufferedReader",
						"Error converting result " + e.toString());
			}
		} catch (Exception ex) {
			Log.e("WebApiCaller - FetchReports Error: ", "Error converting result "
					+ ex.getMessage().toString());
		}
		return array;
	}
	
}
