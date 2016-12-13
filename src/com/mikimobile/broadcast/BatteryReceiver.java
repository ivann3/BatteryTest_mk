package com.mikimobile.broadcast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.mikimobile.application.BatteryTestApplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BatteryReceiver extends BroadcastReceiver {
	
	
	    public Handler mHandler;
	    public Context mContext;  
	    private String mTestData;   //输入文件的数据
		private String filePath = "/mnt/sdcard/batterytest/";   //文件路径
		private String fileName = "batteryResult.txt";     //文件名
		//private DecimalFormat decimalFormat;
		private BufferedWriter bufferedWriter; 
		   
		
		public BatteryReceiver(Handler handler, Context context) {
			this.mHandler=handler;
			this.mContext=context;
			//decimalFormat = new DecimalFormat("0.00");
		}

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if (BatteryTestApplication.start==true) {
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
				
				setLogtime();
				int status = intent.getIntExtra("status", 0);
                int health = intent.getIntExtra("health", 0);
                String technology = intent.getStringExtra("technology");
                
                switch (status) {
                case BatteryManager.BATTERY_STATUS_UNKNOWN:
                	BatteryTestApplication.mStatus = "unknown";
                    break;
                case BatteryManager.BATTERY_STATUS_CHARGING:
                	BatteryTestApplication.mStatus = "charging";
                    break;
                case BatteryManager.BATTERY_STATUS_DISCHARGING:
                	BatteryTestApplication.mStatus = "discharging";
                    break;
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                	BatteryTestApplication.mStatus = "not charging";
                    break;
                case BatteryManager.BATTERY_STATUS_FULL:
                	BatteryTestApplication.mStatus = "full";
                    break;
                }
                
                switch (health) {
                case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                	BatteryTestApplication.mHealth = "unknown";
                    break;
                case BatteryManager.BATTERY_HEALTH_GOOD:
                	BatteryTestApplication.mHealth = "good";
                    break;
                case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                	BatteryTestApplication.mHealth = "overheat";
                    break;
                case BatteryManager.BATTERY_HEALTH_DEAD:
                	BatteryTestApplication.mHealth = "dead";
                    break;
                case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                	BatteryTestApplication.mHealth = "voltage";
                    break;
                case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                	BatteryTestApplication.mHealth = "unspecified failure";
                    break;
                }
				BatteryTestApplication.mBatteryEnergy = intent.getIntExtra("level", 0);				
				BatteryTestApplication.mBatteryVoltage = intent.getIntExtra("voltage", 0);			
				//BatteryTestApplication.mBatteryTemperature = 0.1*intent.getIntExtra("temperature", 0);
				
				if (!BatteryTestApplication.mTechnology.equals(technology)) {
					BatteryTestApplication.mTechnology=technology;
				}
				
				if (BatteryTestApplication.VideoMode==false) {
					Message msg4 = mHandler.obtainMessage(4);
					mHandler.sendMessageDelayed(msg4, 500);
				}
				
				if (status==BatteryManager.BATTERY_STATUS_NOT_CHARGING || status==BatteryManager.BATTERY_STATUS_DISCHARGING) {
					if (BatteryTestApplication.mBatteryEnergy>=1 && BatteryTestApplication.mBatteryEnergy<3) {
						if (BatteryTestApplication.VideoMode) {
							Message msg12 = mHandler.obtainMessage(12);
							mHandler.sendMessageDelayed(msg12, 500);
						}else {
							Message msg11 = mHandler.obtainMessage(11);
							mHandler.sendMessageDelayed(msg11, 500);
						}
					}
				}
				
				
				   
			}
				
								
			if (action.equals(Intent.ACTION_SHUTDOWN)) {
				BatteryTestApplication.Mode="Shutdown";
				mTestData = BatteryTestApplication.Mode+"  "+"Level:"+BatteryTestApplication.mBatteryEnergy
					    +"%    Voltage:"+BatteryTestApplication.mBatteryVoltage+" mv"
					    +"  "+"Health:"+BatteryTestApplication.mHealth
					    +"  "+"Status:"+BatteryTestApplication.mStatus
					    +"   LogTime:"+BatteryTestApplication.mBatteryLogtime+"\r\n";
				saveFile();
				checkpicture();
			}
				
			if (action.equals(Intent.ACTION_SCREEN_OFF)) {
				BatteryTestApplication.standbyMode=true;
				BatteryTestApplication.Mode="Standby";
				if (BatteryTestApplication.VideoMode==false) {				
					Message msg1 = mHandler.obtainMessage(1);
				    mHandler.sendMessage(msg1);	
				}else {					
					Message msg9 = mHandler.obtainMessage(3);
				    mHandler.sendMessage(msg9);	
				}
					
			} 
			
			if (action.equals(Intent.ACTION_SCREEN_ON)) {
				BatteryTestApplication.standbyMode=false;
				
				if (BatteryTestApplication.VideoMode==false) {
					Message msg2 =mHandler.obtainMessage(2);
				    mHandler.sendMessage(msg2);      
				}else {
					Message msg10 = mHandler.obtainMessage(10);
				    mHandler.sendMessage(msg10);	
				}
				   
			}
			
		}
		
	}
 
	/**
     *记录当前时间
     */
    private void setLogtime() {
		 Date nowTime = new Date(System.currentTimeMillis());
		 SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss");
		 BatteryTestApplication.mBatteryLogtime = sdFormatter.format(nowTime);
	}
    
    /**
     *保存记录文件
     */   
    private void saveFile() {
		String strFilePath = filePath+fileName;
		OutputStreamWriter write = null;
	    try {
	    	 File file = new File(strFilePath);
	    	 if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();	
			}
	    	 write = new OutputStreamWriter(new FileOutputStream(file, true),Charset.forName("UTF-8"));//一定要使用gbk格式
	    	 //writer =new FileWriter(file, true);
	  
	    	 bufferedWriter= new BufferedWriter(write);
	    	 if (mTestData!=null /*&& BatteryTestApplication.mBatteryVoltage!=0*/) {
	    		 bufferedWriter.write(mTestData);
	    		 Log.e("mylog", "保存一条信息");
		    	 // 每次写入时，都换行写
		    	 bufferedWriter.newLine();
		    	 bufferedWriter.flush(); 
			}    	 
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
    }   
	    /**
	     *检查文件
	     */ 
	    private void checkpicture() {
	    	File picture = new File(filePath, "batteryGraphics.png");
	    	if (!picture.exists()) {
	    		if (BatteryTestApplication.VideoMode) {
					Message msg12 = mHandler.obtainMessage(12);
					mHandler.sendMessageDelayed(msg12, 500);
				}else {
					Message msg11 = mHandler.obtainMessage(11);
					mHandler.sendMessageDelayed(msg11, 500);
				}
					
			}
		}
	

    
     
    
}
