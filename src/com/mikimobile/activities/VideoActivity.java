package com.mikimobile.activities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import com.mikimobile.application.BatteryTestApplication;
import com.mikimobile.batterytest.R;
import com.mikimobile.broadcast.BatteryReceiver;
import com.mikimobile.service.BatteryTestService;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

public class VideoActivity extends Activity implements OnClickListener{
	
	private VideoView mVideoView;
	private ImageView backButton;
	private View actionbar;
	private MediaController mediaController; //进度条
	private Handler videohHandler;
	
	private IntentFilter mIntentFilter;     
	private BatteryReceiver batteryReceiver;
	
	private String mTestData="";   //输入文件的数据
	private String filePath = "/mnt/sdcard/batterytest/";   //文件路径
	private String fileName = "batteryResult.txt";     //文件名
	
	public BufferedWriter bufferedWriter;
		
	//private DecimalFormat decimalFormat;   //数据格式   
	private double[] xV;
    private double[] yV;
	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();    
	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();   
	private XYSeries mXYSeries;  
	private XYSeriesRenderer mXYRenderer = new XYSeriesRenderer(); 
	private GraphicalView mChartView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.video_view);
			
		Log.e("mylog", "Video is onCreate");
		
		if (BatteryTestApplication.start==false) {
			BatteryTestApplication.start=true;
		}
		
		mVideoView=(VideoView)findViewById(R.id.mVideoView);
		actionbar=(View)findViewById(R.id.actionbar);
		backButton=(ImageView)findViewById(R.id.actionbar_prev);
		backButton.setOnClickListener(this);
		initChartPropety();
		//decimalFormat = new DecimalFormat("0.00");
		initVideo();
		setVideoViewPosition();
		mVideoView.start();
		
		videohHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				switch (msg.what) {
				case 7:
					mTestData = BatteryTestApplication.Mode+"  "+"Level:"+BatteryTestApplication.mBatteryEnergy
				    +"%    Voltage:"+BatteryTestApplication.mBatteryVoltage+" mv"
				    +"  "+"Health:"+BatteryTestApplication.mHealth
				    +"  "+"Status:"+BatteryTestApplication.mStatus
				    +"   LogTime:"+BatteryTestApplication.mBatteryLogtime+"\r\n";
				    				
				    if (videohHandler!=null) {
					    videohHandler.postDelayed(new Runnable() {	
						      @Override
						   public void run() {
						     // 保存文件，重新等待5分钟
						     saveFile();
						     add_Data_for_chart();
						     Message msg8 = obtainMessage(8);
						       if (videohHandler!=null) {
							       videohHandler.sendMessage(msg8);
						       }					   
						   }
					     }, 500);
				    }
					break;
				
				case 8:
					// 5分钟记录一次
					if (videohHandler!=null) {
					    Message msg7 = obtainMessage(7);
					    videohHandler.sendMessageDelayed(msg7, 5*60*1000);
					}
					break;
				
				case 9:
					if (videohHandler!=null) {
						videohHandler.removeMessages(7);
						videohHandler.removeMessages(8);
					}
					
					Intent intent = new Intent(VideoActivity.this,BatteryTestService.class);
					startService(intent);
					mVideoView.pause();
					break;
				
				case 10:				
					Intent intent1 = new Intent(VideoActivity.this,BatteryTestService.class);
					stopService(intent1);
					BatteryTestApplication.Mode="Video Mode";
					if (videohHandler!=null) {
						Message msg10 = obtainMessage(8);
						videohHandler.sendMessage(msg10);
					}
				    mVideoView.start();
					break;
				case 12:
					add_Data_for_chart();
					BuildChartInNotcharge();
					mChartView=null;
					break;
				default:
					break;
				}
				super.handleMessage(msg);
			}
		};
		
		registerBroadcast();	
		
		
		mVideoView.setOnCompletionListener(new OnCompletionListener() {			
			@Override
			public void onCompletion(MediaPlayer mp) {
				// 监听VideoView，循环播放
				mVideoView.start();
			}
		});
				
		Message msg = videohHandler.obtainMessage(7);
		videohHandler.sendMessageDelayed(msg, 10000);
		
	}
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.actionbar_prev:
			mVideoView.pause();
			finish();
			break;

		default:
			break;
		}
	}
	
   @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    	// TODO Auto-generated method stub
	    if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction()==KeyEvent.ACTION_DOWN) {
	    	mVideoView.pause();
			finish();
	   	  }
	    	return super.onKeyDown(keyCode, event);
	    }
		
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		BatteryTestApplication.VideoMode=false;
		if (mVideoView != null) {
			mVideoView.suspend();
		}
		
		if (videohHandler!= null) {
			videohHandler = null;
		} 	
		unregisterReceiver(batteryReceiver);
		
		try {   		
    	    if (bufferedWriter!=null) {
    		    bufferedWriter.close();
			    bufferedWriter=null;
		    }
    	    
    	    mChartView=null;
            mDataset.clear();
            mXYSeries.clear();
            
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
 	 * 初始化 VideoView
 	 */    
     private void initVideo() {
		try {
			mediaController = new MediaController(this){
				@Override
				public void hide() {
					// 重写hide方法	
					super.hide();
					if (actionbar.isShown()) {
						actionbar.setVisibility(View.GONE);
					}
					
				}
				@Override
				public void show() {
					// 重写show方法
					super.show();
					if (!actionbar.isShown()) {
						actionbar.setVisibility(View.VISIBLE);
						actionbar.bringToFront();
					}
				}
			};
			mVideoView.setMediaController(mediaController);
	        mVideoView.setVideoURI(BatteryTestApplication.getInstance().getVideoUri());		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
     
     /**
      * 设置视频大小位置
      */
     private void setVideoViewPosition() {
    	 RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                 RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);              
         //params.addRule(RelativeLayout.CENTER_IN_PARENT);
    	 params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
         params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
         params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
         params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
         mVideoView.setLayoutParams(params);//设置VideoView的布局参数
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
 	    	 if (mTestData!=null) {
 	    		 bufferedWriter.write(mTestData);
 	    		 Log.e("mylog", "保存一条信息");
 		    	 // 每次写入时，都换行写
 		    	 bufferedWriter.newLine();
 		    	 bufferedWriter.flush(); 
 			}    	 
 		} catch (Exception e) {
 			// TODO: handle exception
 			e.printStackTrace();
 			Toast.makeText(VideoActivity.this, "Exception："+e.getMessage(), Toast.LENGTH_SHORT).show();
 		}
 	}
     
     private void registerBroadcast() {
     	batteryReceiver=new BatteryReceiver(videohHandler,this);
 		mIntentFilter=new IntentFilter();
 		mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
 		mIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
 		mIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
 		mIntentFilter.addAction(Intent.ACTION_SHUTDOWN);
 		mIntentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
 		registerReceiver(batteryReceiver, mIntentFilter);		
 		
 	}
     /**
      *生成图表的横坐标数据
      */  
     private void add_Data_for_chart() {
     	BatteryTestApplication.mEnergySizeList.add(BatteryTestApplication.xTimes/60.0);
     	BatteryTestApplication.mEnergyValuesList.add((double)BatteryTestApplication.mBatteryEnergy);
 	}
     
     private void  BuildChartInNotcharge() {
    	 //判断：如果在放电状态下，电量达到5%或2%，创建图片
		 if (BatteryTestApplication.mEnergySizeList.size()>0) {
		     xV = new double[BatteryTestApplication.mEnergySizeList.size()];
		     yV = new double[BatteryTestApplication.mEnergyValuesList.size()];
		     for (int i = 0; i < xV.length; i++) {
		     	  xV[i] = BatteryTestApplication.mEnergySizeList.get(i);   // java 1.5+ style (outboxing)
		     	  yV[i] = BatteryTestApplication.mEnergyValuesList.get(i);
		     }		
  				  if (xV!=null && xV.length>0) {
					  Log.e("mylog", "生成图片");
					  buildChart(VideoActivity.this, xV, yV);
				  }   		 	 
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
