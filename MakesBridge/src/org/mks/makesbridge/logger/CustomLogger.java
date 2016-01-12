package org.mks.makesbridge.logger;

import android.content.Context;

import com.logentries.android.AndroidLogger;

public class CustomLogger {

	static AndroidLogger logger;
	
	public static void Initialize(Context context)
	{
		logger = AndroidLogger.getLogger(context, "a9bb9cb1-e872-465f-a548-2417a401fcbb");
	}
	
	public static void LogError(String msg)
	{
		logger.error(msg);
	}
	
}
