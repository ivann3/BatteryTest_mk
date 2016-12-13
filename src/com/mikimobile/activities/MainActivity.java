package com.mikimobile.activities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;

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
import com.mikimobile.util.FileUtils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity implements OnClickListener {

	
    private static final int CHOOSE_VIDEO_FILE=1;
    private static final int CHOOSE_AUDIO_FILE=2;
  
    private static int nowBrightnessValue; //原本的系统亮度
    private static PowerManager.WakeLock wakeLock;  //电源锁
    private long exitTime = 0;  //退出程序时间
    private boolean isRecord = false;
    private double[] xV;
    private double[] yV;
	private String mTestData;   //输入文件的数据
	private String filePath = "/mnt/sdcard/batterytest/";   //文件路径
	private String fileName = "batteryResult.txt";     //文件名
	
	private IntentFilter mIntentFilter;     
	private BatteryReceiver batteryReceiver;
	
	private TextView mVoltage;
	private TextView mDumpEnergy;
	private TextView mStatus;
	private TextView mHealth;
	private TextView mTechnology;
	private TextView mMusic_uri;
	private TextView mVideo_uri;
	
	private Button normal;
	private Button exit;
	private Button reset;
	private Button stop;
	private Button musicMode;
	private Button videoMode;
	
	private Handler mHandler=null;
	
	public BufferedWriter bufferedWriter;
	//public FileWriter writer;
	
	//private DecimalFormat decimalFormat;   //数据格式   
    // private WifiManager wifiManager;
    //private BluetoothAdapter bluetoothAdapter;
    private MediaPlayer mediaPlayer;
    
    private Uri deafult_videoUri;
    private Uri deafult_musicUri;
    
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();    
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();   
	private XYSeries mXYSeries;  
	private XYSeriesRenderer mXYRenderer = new XYSeriesRenderer(); 
	private GraphicalView mChartView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        initView();
        init();
		initMediaPlayer(deafult_musicUri);
		initChartPropety();
        
		mHandler = new Handler(){
        	@Override
        	public void handleMessage(Message msg) {
        		// TODO Auto-generated method stub
        		switch (msg.what) {
				case 0:
					if (isRecord) {
						mTestData = BatteryTestApplication.Mode+"  "+"Level:"+BatteryTestApplication.mBatteryEnergy
								    +"%    Voltage:"+BatteryTestApplication.mBatteryVoltage+" mv"
								    +"  "+"Health:"+BatteryTestApplication.mHealth
								    +"  "+"Status:"+BatteryTestApplication.mStatus
								    +"   LogTime:"+BatteryTestApplication.mBatteryLogtime+"\r\n";
						
						mHandler.postDelayed(new Runnable() {	
							@Override
							public void run() {
								// 保存数据到TXT文件，保存以后重新等待5分钟
								saveFile();
								add_Data_for_chart(); //图表数据
								Message msg3 = obtainMessage(3);
							    mHandler.sendMessage(msg3);			
							}
						}, 500);
						
						
					}					
					break;
				case 1:
					if (isServiceRunning(MainActivity.this, "com.mikimobile.service.BatteryTestService")) {
						Log.e("mylog", "Service is running");
					}else {
						isRecord=false; 
						if (mHandler!=null) {
							mHandler.removeMessages(0);
				        	mHandler.removeMessages(3);
						}  	
						BatteryTestApplication.ChargeMode=false;
						BatteryTestApplication.MusicMode=false;
						if (mediaPlayer.isPlaying()) {
							mediaPlayer.pause();
						}				
						BatteryTestApplication.getInstance().setAlarm(true);		
						Intent intent = new Intent(MainActivity.this,BatteryTestService.class);
						startService(intent);
					}
					break;
				case 2:
					    BatteryTestApplication.getInstance().setAlarm(false);
				        Intent intent1 = new Intent(MainActivity.this,BatteryTestService.class);
				        stopService(intent1);    	     
					break;			
				case 3:
					if (isRecord) {
						Message msg0 = obtainMessage(0);   // 5分钟记录一次
					    mHandler.sendMessageDelayed(msg0, 5*60*1000);
					}				
					break;
				case 4:
					if (isRecord) {
						Datadispaly();
					}			
					break;
				case 5:
					 getArray();
		        	 if (xV!=null && xV.length>0) {
		        		 buildChart(MainActivity.this, xV, yV);  	
		    		 }
					finish();
					break;
				case 6:
					BatteryTestApplication.Mode="Video Mode";
		        	BatteryTestApplication.VideoMode=true;
		        	BatteryTestApplication.ChargeMode=false;		
					BatteryTestApplication.MusicMode=false;
					if (mediaPlayer.isPlaying()) {
						mediaPlayer.pause();
					}
		        	isRecord=false; 
		        	if (mHandler!=null) {
						mHandler.removeMessages(0);
			        	mHandler.removeMessages(3);
					}
		        	Intent videoIntent =new Intent(MainActivity.this,VideoActivity.class);
		            startActivity(videoIntent);        	
					break;
				case 11:
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
      
        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
			
			@Override
			public void onCompletion(MediaPlayer mp) {
				// 循环播放音乐
				mediaPlayer.start();
			}
		});
    }
       
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	// TODO Auto-generated method stub
    	if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction()==KeyEvent.ACTION_DOWN) {
			if (System.currentTimeMillis()-exitTime>2000) {
				Toast.makeText(MainActivity.this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
				exitTime=System.currentTimeMillis();
				
			} else {
				   close();    
				   Message msg = mHandler.obtainMessage(5);
		           mHandler.sendMessageDelayed(msg, 500);
			}
			return true;
		} 
    	
    	       
    	return super.onKeyDown(keyCode, event);
    }
    
    
    @Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.normal_mode:
			BatteryTestApplication.Mode="Charge Mode";
			if (BatteryTestApplication.ChargeMode) {
				Toast.makeText(MainActivity.this, "充电测试模式，正在记录", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, "进入充电测试，开始记录电量，请插入充电器", Toast.LENGTH_SHORT).show();
				BatteryTestApplication.ChargeMode=true;
				BatteryTestApplication.VideoMode=false;
				BatteryTestApplication.MusicMode=false;
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.pause();
		        }				
			}
			
			if (BatteryTestApplication.start!=true) {
				BatteryTestApplication.start=true;     	    
			}
			
			if (isRecord==false) {
				isRecord=true;
				if (mHandler!=null) {
					mHandler.removeMessages(0);
					mHandler.removeMessages(3);
					Message msg = mHandler.obtainMessage(0);
					mHandler.sendMessageDelayed(msg, 10000);		
				}			
			 }				
			  
			break;
        
		case R.id.music_mode: 
			BatteryTestApplication.Mode="Music Mode";
			if (BatteryTestApplication.MusicMode) {
				Toast.makeText(MainActivity.this, "音乐测试模式，正在记录", Toast.LENGTH_SHORT).show();	
			}else {
				Toast.makeText(this, "进入音乐放电测试，开始记录电量，请拔出充电器", Toast.LENGTH_SHORT).show();
				BatteryTestApplication.MusicMode=true;
				BatteryTestApplication.VideoMode=false;
				BatteryTestApplication.ChargeMode=false;
				
				if (!mediaPlayer.isPlaying()) {
	       		     mediaPlayer.start();     
				}		
			}
        	  	
        	if (BatteryTestApplication.start!=true) {
				BatteryTestApplication.start=true;     	    
			}
        	
        	if (isRecord==false) {
				isRecord=true;
				if (mHandler!=null) {
					mHandler.removeMessages(0);
					mHandler.removeMessages(3);
					Message msg = mHandler.obtainMessage(0);
					mHandler.sendMessageDelayed(msg, 10000);	
				}		
			}
        	     							
			break;
			
        case R.id.video_mode:
        	Toast.makeText(this, "进入视频放电测试，开始记录电量，请拔出充电器", Toast.LENGTH_SHORT).show();
        	Message msg6 = mHandler.obtainMessage(6);
        	mHandler.sendMessage(msg6); 	
			break;
        case R.id.music_uri: 	
        	showFileChooser(CHOOSE_AUDIO_FILE);
            break;
        case R.id.video_uri:
        	showFileChooser(CHOOSE_VIDEO_FILE);
            break;
			
        case R.id.stop:
        	isRecord=false; //不记录数据，关机数据除外，计时器不停止
        	mHandler.removeMessages(0);
        	mHandler.removeMessages(3);
        	BatteryTestApplication.ChargeMode=false;		
			BatteryTestApplication.MusicMode=false;
        	if (mediaPlayer.isPlaying()) {
				mediaPlayer.pause();
			}
        	Toast.makeText(this, "停止记录数据", Toast.LENGTH_SHORT).show();	
	        break;
        case R.id.reset:      	
        	reset();        	
        	deleteFile();      		
			break;
        
        case R.id.exit:
        	 close();    
			 Message msg = mHandler.obtainMessage(5);
	         mHandler.sendMessageDelayed(msg, 500);   			
			break;
			
		default:
			break;
		}
	}
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	// TODO Auto-generated method stub
    	switch (requestCode) {
		case CHOOSE_VIDEO_FILE:
			if (resultCode==Activity.RESULT_OK) {
				Uri uri = data.getData(); 
				BatteryTestApplication.getInstance().setVideoUri(uri);		
				mVideo_uri.setText(FileUtils.getFileName(FileUtils.getPath(this, uri)));		
			}
			break;
		case CHOOSE_AUDIO_FILE:
			if (resultCode==Activity.RESULT_OK) {
				Uri uri = data.getData(); 
				BatteryTestApplication.getInstance().setMusicUri(uri);
				mMusic_uri.setText(FileUtils.getFileName(FileUtils.getPath(this, uri)));
				mediaPlayer.reset();
				initMediaPlayer(uri);
				if (BatteryTestApplication.MusicMode) {
					mediaPlayer.start();
				}
				
			}
			break;

		default:
			break;
		}
    	super.onActivityResult(requestCode, resultCode, data);
    }
    
    private void initView() {
    	mVoltage=(TextView)findViewById(R.id.battery_voltage);
    	mDumpEnergy=(TextView)findViewById(R.id.battery_energy);
    	mStatus=(TextView)findViewById(R.id.battery_status);
    	mHealth=(TextView)findViewById(R.id.battery_health);
    	mTechnology=(TextView)findViewById(R.id.battery_technology);
    	mMusic_uri=(TextView)findViewById(R.id.music_uri);
    	mVideo_uri=(TextView)findViewById(R.id.video_uri);
    	
    	normal=(Button)findViewById(R.id.normal_mode);
    	videoMode=(Button)findViewById(R.id.video_mode);
    	musicMode=(Button)findViewById(R.id.music_mode);
    	stop=(Button)findViewById(R.id.stop);
    	reset=(Button)findViewById(R.id.reset);
    	exit=(Button)findViewById(R.id.exit);
		
		normal.setOnClickListener(this);			
		videoMode.setOnClickListener(this);
		musicMode.setOnClickListener(this);	
		stop.setOnClickListener(this);
		reset.setOnClickListener(this);
		exit.setOnClickListener(this);
		mMusic_uri.setOnClickListener(this);
		mVideo_uri.setOnClickListener(this);
		
		mMusic_uri.setText("deafult");
		mVideo_uri.setText("deafult");		
    }
    
    private void registerBroadcast() {
    	batteryReceiver=new BatteryReceiver(mHandler,MainActivity.this);
		mIntentFilter=new IntentFilter();
		mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
		mIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		mIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
		mIntentFilter.addAction(Intent.ACTION_SHUTDOWN);
		mIntentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		registerReceiver(batteryReceiver, mIntentFilter);		
		
	}
  
    @Override
    protected void onDestroy() {
    	// TODO Auto-generated method stub
    	super.onDestroy();
    	Log.e("mylog", "main_activity is onDestroy.");    	
    	
    	if (mediaPlayer!=null) {
			mediaPlayer.stop();
			mediaPlayer.release();
		} 	
    	if (mHandler!=null) {
			mHandler=null;
		} 	
		unregisterReceiver(batteryReceiver);
    }
      
    /**
     *显示数据
     */  
    private void Datadispaly() {
		mVoltage.setText(""+BatteryTestApplication.mBatteryVoltage+" mv");
		mDumpEnergy.setText(BatteryTestApplication.mBatteryEnergy+"%");
		mStatus.setText(""+BatteryTestApplication.mStatus);
		mHealth.setText(BatteryTestApplication.mHealth);
		mTechnology.setText(BatteryTestApplication.mTechnology);
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
			Toast.makeText(MainActivity.this, "Exception："+e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
    
    /**
     *删除文件
     */
    private void deleteFile(){
        File file = new File(filePath+fileName);
        File pictureFile = new File(filePath+"batteryGraphics.png");
        if(file.exists()&&file.isFile()){
            file.delete();
            if (file.exists()) {
            	Toast.makeText(MainActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(MainActivity.this, "文件已删除", Toast.LENGTH_SHORT).show();
			       }           
        }else {
			Toast.makeText(MainActivity.this, "文件不存在", Toast.LENGTH_SHORT).show();
		}
        if (pictureFile.exists()&&pictureFile.isFile()) {
			pictureFile.delete();
		}
    }
    
    /**
	 * 保持屏幕处于亮屏状态
	 */
	@SuppressWarnings("deprecation")
	private void keepScreenOn() {
		PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK| PowerManager.ACQUIRE_CAUSES_WAKEUP, "Power");
		wakeLock.acquire();
	}
	
	/**
	 * 释放电源锁
	 */
	private void releaseWakeLock() {
		if (wakeLock!=null) {
	    	   try {
	    		   wakeLock.release();
	    		   wakeLock=null;
			} catch (Exception e) {
				Log.e("mylog", e.getMessage());
			}
 			
 	}
	}
	/*
	/**
	 * wifi开关
	 *
	private void WifiSwitch(boolean enabled) {
		if (enabled) {
			if (!wifiManager.isWifiEnabled()) {
				wifiManager.setWifiEnabled(true);
			}
		}else {
			if (wifiManager.isWifiEnabled()) {
				wifiManager.setWifiEnabled(false);
			}
		}	
	}*/
	
	/*
	/**
	 * 蓝牙开关
	 *
	private void BluetoothSwitch(boolean enabled) {
		if (enabled) {
			if (!bluetoothAdapter.isEnabled()) {
				bluetoothAdapter.enable();
			}
		}else {
			if (bluetoothAdapter.isEnabled()) {
				bluetoothAdapter.disable();
			}
		}	
	}*/
	
	/**
	 * 清理数据显示界面
	 */
	private void clearDisplay() {		
		mVoltage.setText("");	
	    mDumpEnergy.setText("");
	    mStatus.setText("");
	    mHealth.setText("");
	}
	
	/**
	 * 初始化 MediaPlayer
	 */
     private void initMediaPlayer(Uri uri) {
		try {
			mediaPlayer.setDataSource(MainActivity.this, uri);
			mediaPlayer.prepare();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
     
    
     
     /** 
     * 获取屏幕的亮度 
     *  
     * @param activity 
     * @return 
     */  
    public static int getScreenBrightness(Activity activity) {  
        int nowBrightnessValue = 0;  
        ContentResolver resolver = activity.getContentResolver();  
        try {  
            nowBrightnessValue = android.provider.Settings.System.getInt(  
                    resolver, Settings.System.SCREEN_BRIGHTNESS);  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
        return nowBrightnessValue;  
    }  
      
    /** 
     * 设置亮度 
     *  
     * @param activity 
     * @param brightness 
     */  
    public static void setBrightness(Activity activity, int brightness) {  
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();  
        lp.screenBrightness = Float.valueOf(brightness) * (1f / 255f);  
        activity.getWindow().setAttributes(lp);  
    }  

    private void init() {  
    	deafult_videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.router);
        deafult_musicUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.love_story);
    	BatteryTestApplication.getInstance().setMusicUri(deafult_musicUri);
        BatteryTestApplication.getInstance().setVideoUri(deafult_videoUri);
    	
        nowBrightnessValue=getScreenBrightness(MainActivity.this);
       /* wifiManager=(WifiManager)getSystemService(Context.WIFI_SERVICE);
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();     
        WifiSwitch(true);
        BluetoothSwitch(true);*/
		keepScreenOn();     
        setBrightness(MainActivity.this, 255);
        mediaPlayer=new MediaPlayer();           
        
	}
    
    /**
	 * 打开文件选择器
	 */ 
    private void showFileChooser(int choose) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        if (choose==CHOOSE_VIDEO_FILE) {
        	intent.setType("video/*");
		}else if (choose==CHOOSE_AUDIO_FILE) {
			intent.setType("audio/*");
		}        
        intent.addCategory(Intent.CATEGORY_OPENABLE);
     
        try {
        	 if (choose==CHOOSE_VIDEO_FILE) {
        		 startActivityForResult( Intent.createChooser(intent, "Select a File to Upload"), CHOOSE_VIDEO_FILE);
     		}else if (choose==CHOOSE_AUDIO_FILE) {
     			 startActivityForResult( Intent.createChooser(intent, "Select a File to Upload"), CHOOSE_AUDIO_FILE);
     		}        
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.",  Toast.LENGTH_SHORT).show();
        }
    }

    /**
	 * 重置为默认状态
	 */ 
    private void reset() {
    	BatteryTestApplication.start = false; //清空数据，计时器停止
    	isRecord=false;
    	mHandler.removeMessages(0);
    	mHandler.removeMessages(3);
    	BatteryTestApplication.mEnergySizeList.clear();
    	BatteryTestApplication.mEnergyValuesList.clear();
    	BatteryTestApplication.xTimes=0;
    	BatteryTestApplication.mBatteryEnergy=0;
    	BatteryTestApplication.mBatteryVoltage=0;
    	BatteryTestApplication.VideoMode=false;
    	BatteryTestApplication.ChargeMode=false;		
		BatteryTestApplication.MusicMode=false;
    	BatteryTestApplication.getInstance().setMusicUri(deafult_musicUri);
        BatteryTestApplication.getInstance().setVideoUri(deafult_videoUri);
        mMusic_uri.setText("deafult");
		mVideo_uri.setText("deafult");
		mediaPlayer.reset();
		initMediaPlayer(deafult_musicUri);
    	clearDisplay();
    	mChartView=null;
    	mDataset.clear();
    	mXYSeries.clear();
	}
    
    private void close() {
    	 isRecord=false;
		 BatteryTestApplication.start=false;
 		 try {
 			/*if (writer!=null ) {
			    writer.close();
				writer=null;}*/
			if (bufferedWriter!=null) {
		 		bufferedWriter.close();
			    bufferedWriter=null;}
		   }catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
 			    
		 setBrightness(MainActivity.this,nowBrightnessValue);
 		 /*WifiSwitch(false);
 	     BluetoothSwitch(false);   */     	
 	     releaseWakeLock();
	}
    
    /**  
     * * 判断某个服务是否正在运行的方法  *   
     * * @param mContext  
     * * @param serviceName  
     * *   是包名+服务的类名（例如：net.loonggg.testbackstage.TestService）  
     * * @return true代表正在运行，false代表服务没有正在运行  */
    
    public static boolean isServiceRunning(Context mContext,String className) {
        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE); 
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(30);
          if (!(serviceList.size()>0)) {
             return false;
           }
          for (int i=0; i<serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className) == true) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }
    /**
     *图表的数据
     */  
    private void add_Data_for_chart() {
    	Log.e("mylog", "图像记录时间："+BatteryTestApplication.xTimes);
    	BatteryTestApplication.mEnergySizeList.add(BatteryTestApplication.xTimes/60.0);
    	BatteryTestApplication.mEnergyValuesList.add((double)BatteryTestApplication.mBatteryEnergy);
	}
    
    private void  BuildChartInNotcharge() {
   	 //判断：如果在放电状态下，电量达到5%或2%，创建图片
		 if (BatteryTestApplication.mEnergySizeList.size()>0) {
			  getArray();
		      if (xV!=null && xV.length>0) {
				  buildChart(MainActivity.this, xV, yV);
			  }   
		  }
	  }		
	
    
    private void getArray() {
    	if (BatteryTestApplication.mEnergySizeList.size()>0) {
    		xV = new double[BatteryTestApplication.mEnergySizeList.size()];
    		yV = new double[BatteryTestApplication.mEnergyValuesList.size()];
    		for (int i = 0; i < xV.length; i++) {
    		    
    		    xV[i] = BatteryTestApplication.mEnergySizeList.get(i);   // java 1.5+ style (outboxing)
    		    yV[i] = BatteryTestApplication.mEnergyValuesList.get(i);
    		 }
		}else {
			Toast.makeText(this, "Data is null", Toast.LENGTH_SHORT).show();
		}
		
	}
    
    public void buildChart(Context context,double[]xV, double[]yV) {
    	
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
    	   Toast.makeText(this, "Data is null", Toast.LENGTH_SHORT).show();
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
             Toast.makeText(this, "图片生成异常"+e.getMessage()+"", Toast.LENGTH_SHORT).show();
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
