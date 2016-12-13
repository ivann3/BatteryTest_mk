package com.mikimobile.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Application;
import android.net.Uri;

public class BatteryTestApplication extends Application {
	
	public static  BatteryTestApplication mApplication;
	
	public static Timer timer;
	public boolean isAlarm = true;
	public static boolean start = false; //全局接收广播的总开关
	
	public static int mBatteryEnergy;              //目前电量  
	public static int mBatteryVoltage;             //电池电压  
	//public static double mBatteryTemperature;         //电池温度  	
	public static String mBatteryLogtime = "";          //记录时间
	public static String mStatus = "";
	public static String mHealth = "";
	public static String mTechnology = "";
	public static double xTimes = 0;
	public Uri video_Uri;
	public Uri music_Uri;
	
	public static String Mode =  "";
	public static boolean VideoMode = false;
	public static boolean MusicMode = false;
	public static boolean ChargeMode = false;
	public static boolean standbyMode = false;
	
	public static List<Double> mEnergySizeList = new ArrayList<Double>();
	public static List<Double> mEnergyValuesList = new ArrayList<Double>();
	
	/**
     *返回应用程序实例
     */
    public static BatteryTestApplication getInstance(){
            return mApplication;       
    } 
	
	@Override
	public void onCreate() {
		// 程序创建的时候执行
		super.onCreate();
		mApplication=this;
		
		timer = new Timer();
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				// 全局计时器
				if (start) {
					xTimes=xTimes+1;
					//Log.e("mylog", "nowTimes:"+xTimes);
				}
			}};
			
			if (timer!=null) {
				timer.schedule(task, 1000, 1000);// 1s后执行task,经过1s再次执行
			}
			
	}
	@Override
	public void onTerminate() {
		// 程序终止的时候执行
		super.onTerminate();
		timer.cancel();
	}
	
	@Override
	public void onLowMemory() {
		// 低内存的时候执行
		super.onLowMemory();
	}
	
	@Override
	public void onTrimMemory(int level) {
		// 程序在内存清理的时候执行
		super.onTrimMemory(level);
	}
	
	public void setAlarm(boolean isAlarm) {
		
		this.isAlarm=isAlarm;
	}

	public boolean getAlarm() {
		
		return isAlarm;
	}
		
    public void setMusicUri(Uri uri) {
		
		this.music_Uri = uri;
	}

	public Uri getMusicUri() {
		
		return music_Uri;
	}
	
    public void setVideoUri(Uri uri) {
		
		this.video_Uri = uri;
	}

	public Uri getVideoUri() {
		
		return video_Uri;
	}
}
