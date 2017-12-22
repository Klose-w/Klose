package com.example.kloselibrary;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by admin on 2017/12/19.
 */

public class Klose
{
	private static Klose mKlose = null;
	Context mContext;
	//用于硬盘缓存
	private DiskLruCache diskLruCache;
	//用于内存缓存
	private LruCache<String,Bitmap> mMemoryCache;
	ThreadPoolExecutor threadPoolExecutor;
	private Klose(Context context)
	{
		mContext=context;
		initCache();
		initDiskCache();
		threadPoolExecutor=new ThreadPoolExecutor(10, 10, 1, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(120));
	}

	public static synchronized Klose with(Context context)
	{
		if(mKlose == null){
			synchronized (Klose.class){
				if(mKlose==null){
					context=context.getApplicationContext();
					mKlose = new Klose(context);
				}
			}
		}
		return mKlose;
	}

	public ImageLoader add(ImageView imageView){
		return new ImageLoader(mKlose,imageView);
	}

	/**
	 * 设置内存缓存大小
	 */
	private void initCache(){
		int maxMenroy=(int)Runtime.getRuntime().maxMemory();
		//当前app运行内存的八分之一
		int cachesize=maxMenroy/8;
		mMemoryCache=new LruCache<String,Bitmap>(cachesize){
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getByteCount();
			}
		};
	}

	/**
	 * 获取文件硬盘缓存路径
	 */
	private File getDiskCacheDir(String name){
		String lj=null;
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())||!Environment.isExternalStorageRemovable()){
			lj=mContext.getExternalCacheDir().getPath();
		}else {
			lj=mContext.getCacheDir().getPath();
		}
		String filesd=lj+File.separator+name;
		Log.e("file", filesd);
		return  new File(filesd);
	}

	/**
	 * 获取app版本号
	 */
	private int getAppVersion(Context context){
		try {
			PackageInfo info=mContext.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return  info.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return 1;
	}

	/**
	 * 创建一个硬盘缓存，大小为6M
	 */
	private void initDiskCache(){
		try {
			File cacheDir=getDiskCacheDir("Klosebitmap");
			if(!cacheDir.exists()){
				cacheDir.mkdirs();
				Log.e("dd","dd");
			}
			diskLruCache=DiskLruCache.open(cacheDir,getAppVersion(mContext),1,6*1024*1024);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Context getContext()
	{
		return mContext;
	}

	public DiskLruCache getDiskLruCache()
	{
		return diskLruCache;
	}

	public LruCache<String, Bitmap> getMemoryCache()
	{
		return mMemoryCache;
	}
}
