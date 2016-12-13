package com.mikimobile.broadcast;

import com.mikimobile.application.BatteryTestApplication;
import com.mikimobile.service.BatteryTestService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, Intent intent) {
		// TODO Auto-generated method stub
		if (BatteryTestApplication.getInstance().getAlarm()==true) {
			Intent i = new Intent(context,BatteryTestService.class);
			context.startService(i);
		}
		
	}

}
