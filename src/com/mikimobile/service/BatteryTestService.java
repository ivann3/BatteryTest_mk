package com.mikimobile.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import com.mikimobile.application.BatteryTestApplication;
import com.mikimobile.batterytest.R;
import com.mikimobile.broadcast.AlarmReceiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.BatteryManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

public class BatteryTestService extends Service {
	
	private int mBatteryEnergy;              //目前电量  
	private int mBatteryVoltage;             //电池电压  
	private String mBatteryLogtime = "";          //记录时间
	private IntentFilter mIntentFilter;
	private BatteryServiceReceiver mbatteryServiceReceiver;
	private boolean isrecord = false;
	private String mTestData;
	private String filePath = "/mnt/sdcard/batterytest/";   //文件路径
	private String fileName = "batteryResult.txt";
	public BufferedWriter bufferedWriter;
	public AlarmManager manager;
	public long triggerAtTime;
	public PendingIntent pi;
	private double[] xV;
    private double[] yV;
	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();    
	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();   
	private XYSeries mXYSeries;  
	private XYSeriesRenderer mXYRenderer = new XYSeriesRenderer(); 
	private GraphicalView mChartView;
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;		
	}
		
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		init();
		initChartPropety();
	}

	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		if (isrecord==false) {
			isrecord=true;
		}
		new Thread(new Runnable() {		
			@Override
			public void run() {
				// TODO Auto-generated method stub
				registerBroadcast();
			}
		}).start();		
		
		
		triggerAtTime = SystemClock.elapsedRealtime() + 5*60*1000;
		manager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
		Log.e("mylog", "进入服务");
		return super.onStartCommand(intent, flags, startId);
			
	}
	
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		isrecord=false;
		BatteryTestApplication.getInstance().setAlarm(false);
    	try {
    		if (bufferedWriter!=null) {
    			bufferedWriter.close();
			}		
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	if (mbatteryServiceReceiver!=null) {
    		unregisterReceiver(mbatteryServiceReceiver);
		}
    	mChartView=null;
        mDataset.clear();
        mXYSeries.clear();
		Log.e("mylog", "服务已经销毁");
	}
	
	
	class BatteryServiceReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if (isrecord==true && action.equals(Intent.ACTION_BATTERY_CHANGED)) {
				String newTime = getLogtime();
				if (!mBatteryLogtime.equals(newTime)){ 
					 mBatteryLogtime=newTime;	
					 mBatteryEnergy = intent.getIntExtra("level", 0);
					 mBatteryVoltage = intent.getIntExtra("voltage", 0);
					int status = intent.getIntExtra("status", 0);
	                int health = intent.getIntExtra("health", 0);
	                
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
													
				mTestData=BatteryTestApplication.Mode+" "
						+"Level:"+mBatteryEnergy
					    +"%   Voltage:"+mBatteryVoltage+" mv"
					    +" "+"Health:"+BatteryTestApplication.mHealth
					    +" "+"Status:"+BatteryTestApplication.mStatus
					    +"  LogTime:"+mBatteryLogtime+"\r\n";
					
				isrecord=false;
				
				saveFile();	
				add_Data_for_chart();
				
				//判断：如果在放电状态下，电量达到5%或2%，创建图片	
				if (status==BatteryManager.BATTERY_STATUS_NOT_CHARGING || status==BatteryManager.BATTERY_STATUS_DISCHARGING) {
					if (BatteryTestApplication.mBatteryEnergy>=1 && BatteryTestApplication.mBatteryEnergy<3) {
						if (BatteryTestApplication.mEnergySizeList.size()>0) {
						    getArray();					
							if (xV!=null && xV.length>0) {
								buildChart(BatteryTestService.this, xV, yV);
							}    	      
						}	
					}
				}
			
				
			} 
		}
				
				if (action.equals(Intent.ACTION_SHUTDOWN)) {
					BatteryTestApplication.Mode="Shutdown";
					mTestData=BatteryTestApplication.Mode+" "
				              +"Level:"+mBatteryEnergy+"%   Voltage:"
				              +mBatteryVoltage+" mv"
		                      +" "+"Health:"+BatteryTestApplication.mHealth
					          +" "+"Status:"+BatteryTestApplication.mStatus
				              +"  LogTime:"+mBatteryLogtime+"\r\n";
					saveFile();
					checkpicture();
				}
			}

	}
	
	private void registerBroadcast() {
		registerReceiver(mbatteryServiceReceiver, mIntentFilter);		
	}
	
	private String getLogtime() {
		 Date nowTime = new Date(System.currentTimeMillis());
		 SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss");
		 String newTime = sdFormatter.format(nowTime);
		 return newTime;
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
    	if (!picture.exists()&&picture.isFile()) {
    		add_Data_for_chart();
    		if (BatteryTestApplication.mEnergySizeList.size()>0) {
			    getArray();					
				if (xV!=null && xV.length>0) {
					buildChart(BatteryTestService.this, xV, yV);
				}    	      
			}	
		}
	}
	
	private void init() {
		mbatteryServiceReceiver = new BatteryServiceReceiver();
		mIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		mIntentFilter.addAction(Intent.ACTION_SHUTDOWN);
		manager = (AlarmManager)getSystemService(ALARM_SERVICE);
		Intent wakeIntent = new Intent(this, AlarmReceiver.class);
		pi = PendingIntent.getBroadcast(this, 0, wakeIntent, 0);
	}
	
	/**
     *图表的数据
     */  
    private void add_Data_for_chart() {
    	Log.e("mylog", "服务记录时间"+BatteryTestApplication.xTimes/60.0);
    	BatteryTestApplication.mEnergySizeList.add(BatteryTestApplication.xTimes/60.0);
    	BatteryTestApplication.mEnergyValuesList.add((double)mBatteryEnergy);
	}
    
    private void getArray() {
    	if (BatteryTestApplication.mEnergySizeList.size()>0) {
    		xV = new double[BatteryTestApplication.mEnergySizeList.size()];
    		yV = new double[BatteryTestApplication.mEnergyValuesList.size()];
    		for (int i = 0; i < xV.length; i++) {
    		    //target[i] = doubles.get(i).doubleValue();  // java 1.4 style
    		    // or:
    		    xV[i] = BatteryTestApplication.mEnergySizeList.get(i);   // java 1.5+ style (outboxing)
    		    yV[i] = BatteryTestApplication.mEnergyValuesList.get(i);
    		 }
    		Log.e("mylog", ""+"图像最后记录时间："+xV[xV.length-1]);
    		Log.e("mylog", ""+"图像最后记录的值："+yV[yV.length-1]);
		}else {
			Log.e("mylog", "Data is null");
		}
		
	}
     
     private void buildChart(Context context,double[]xV, double[]yV) {  
 
 	    int seriesLength = xV.length;
 	   
 	    for (int k = 0; k < seriesLength; k++) {
 	      mXYSeries.add(xV[k], yV[k]);   /* 将该条曲线的 x,y 轴数组存放到 单条曲线数据中 */
 	    }
 	    mDataset.addSeries(mXYSeries);
 	    
 	    if (mChartView==null) {
 			mChartView = ChartFactory.getLineChartView(context, mDataset, mRenderer);
 			saveBitmap();
 		}
 	}
    
     private void saveBitmap() {
      	//生成图片保存
         Bitmap bitmap = convertViewToBitmap(mChartView);
         if (bitmap==null) {
         	Log.e("mylog", "Data is null");
  	   }else {
  		FileOutputStream output =null;
          try {  
            File pic = new File(filePath, "batteryGraphics.png");  
            if (pic.exists()) {
          	    pic.delete();
  		  }
            output = new FileOutputStream(pic);  
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            output.flush();
    		output.close();
          } catch (Exception e) {  
            e.printStackTrace();
          } 
  	   }
         
  	}
    
     public Bitmap convertViewToBitmap(View view){  
         
         Bitmap bitmap = Bitmap.createBitmap(1270, 300,    
                 Bitmap.Config.ARGB_8888);    
         //利用bitmap生成画布    
         Canvas canvas = new Canvas(bitmap);  
           
         //把view中的内容绘制在画布上    
         view.draw(canvas);            
           
     return bitmap;  
  }     
     
     private void initChartPropety() {
    	mRenderer.setApplyBackgroundColor(true);//设置是否显示背景色  
 	    mRenderer.setBackgroundColor(getResources().getColor(R.color.lightblack));//设置背景色  
 	    mRenderer.setAxisTitleTextSize(10); //设置轴标题文字的大小  
 	    mRenderer.setXTitle("minute");
 	    mRenderer.setYTitle("level");
 	    mRenderer.setChartTitle("电池测试数据-电量");
 	    mRenderer.setChartTitleTextSize(15);//?设置整个图表标题文字大小  
 	    mRenderer.setLabelsTextSize(8);//设置刻度显示文字的大小(XY轴都会被设置)  
 	    mRenderer.setLegendTextSize(12);//图例文字大小  
 	    mRenderer.setLegendHeight(10);
 	    mRenderer.setMargins(new int[] { 35, 20, 25, 20 });//设置图表的外边框(上/左/下/右) 
 	    mRenderer.setPointSize(2);//设置点的大小(图上显示的点的大小和图例中点的大小都会被设置)
 	    mRenderer.setXAxisMin(0);
 	    mRenderer.setXAxisMax(360);
 	    mRenderer.setYAxisMin(0);
 	    mRenderer.setYAxisMax(100);
 	    mRenderer.setXLabels(72);
 	    mRenderer.setYLabels(10);  /* 设置 y 轴刻度个数 */
 	    //mRenderer.setPanEnabled(false, false);//允许X轴可拉动  
        mRenderer.setZoomEnabled(false, false);//设置不可缩放  
        mRenderer.setShowGrid(true);
 	    
 	    
 	    mXYRenderer.setColor(getResources().getColor(R.color.green));//设置颜色  
 	    mXYRenderer.setPointStyle(PointStyle.CIRCLE);//设置点的样式  
 	    mXYRenderer.setFillPoints(true);//填充点（显示的点是空心还是实心）  
 	    mXYRenderer.setDisplayChartValues(true);//将点的值显示出来  
 	    mXYRenderer.setChartValuesSpacing(5);//显示的点的值与图的距离  
 	    mXYRenderer.setChartValuesTextSize(10);//点的值的文字大小  
 	    mXYRenderer.setLineWidth(1);//设置线宽  
 	    mRenderer.addSeriesRenderer(mXYRenderer);
 	    
 	    mXYSeries = new XYSeries("level");
 	}
}

