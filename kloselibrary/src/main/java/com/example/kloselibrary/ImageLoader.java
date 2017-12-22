package com.example.kloselibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by admin on 2017/12/19.
 */

public class ImageLoader
{
	//是否压缩
	private boolean mIsZip = true;
	private Klose mKlose;
	private ImageView mImageView;
	private MD5Util mMD5Util;
	//不指定压缩size的默认值
	private int mHeight = -1;
	private int mWidth = -1;
	//加载失败图片
	private int failLoadImage = R.drawable.error;
	//加载失败图片
	private int failLoadBitmap;
	//加载中图片
	private int LoadingImage = R.drawable.ing;
	//加载中图片
	private Bitmap LoadingBitmap;
	public ImageLoader(Klose klose,ImageView imageView){
		mKlose=klose;
		mImageView=imageView;
		mMD5Util=new MD5Util();
		LoadingBitmap = BitmapFactory.decodeResource(mKlose.getContext().getResources(),LoadingImage);
	}
	public ImageLoader fail(int resource){
		failLoadImage = resource;
		return this;
	}

	public ImageLoader size(int height,int width){
		if (height<=0 || width <=0){
			return this;
		}
		mHeight = height;
		mWidth = width;
		return this;
	}
	public ImageLoader zip(boolean isZip){
		mIsZip = isZip;
		return this;
	}
	public ImageLoader load(String url){
		mImageView.setImageBitmap(LoadingBitmap);
		String key = mMD5Util.getMD5(url);
		Bitmap bitmap=mKlose.getMemoryCache().get(key);
		if(bitmap==null){
			BitmapWorkerTask bitmapWorkerTask=new BitmapWorkerTask();
			bitmapWorkerTask.executeOnExecutor(mKlose.threadPoolExecutor,key,url);
		}else {
			mImageView.setImageBitmap(bitmap);
		}
		return this;
	}
	class  BitmapWorkerTask extends AsyncTask<String,Void,Bitmap>
	{
		String mUrl;
		String mKey;

		@Override
		protected Bitmap doInBackground(String... params)
		{
			mKey = params[0];
			mUrl = params[1];
			FileDescriptor fileDescriptor = null;
			FileInputStream fileInputStream = null;
			DiskLruCache.Snapshot snapshot = null;
			try
			{
				snapshot = mKlose.getDiskLruCache().get(mKey);
				if (snapshot == null)
				{
					DiskLruCache.Editor editor = mKlose.getDiskLruCache().edit(mKey);
					if (editor != null)
					{
						OutputStream outputStream = editor.newOutputStream(0);
						if (downloadUrlToStream(mUrl, outputStream))
						{
							Log.e("jjjjj", "kkk");
							editor.commit();
						}
						else
						{
							editor.abort();
							return getBitmap(failLoadImage);
						}
					}
					snapshot = mKlose.getDiskLruCache().get(mKey);
				}
				if (snapshot != null)
				{
					Log.e("jjjjj", "kkk1");
					fileInputStream = (FileInputStream) snapshot.getInputStream(0);
					fileDescriptor = fileInputStream.getFD();
				}
				Bitmap bitmap = null;
				if (fileDescriptor != null)
				{
					Log.e("jjjjj", "kkk2");
					if (mIsZip){
						bitmap = getBitmap(fileDescriptor);
					}else {
						bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
					}

				}
				if (bitmap != null)
				{
					Log.e("jjjjj", "kkk3");
					mKlose.getMemoryCache().put(mKey, bitmap);
				}
				return bitmap;
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			return getBitmap(failLoadImage);
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			super.onPostExecute(bitmap);
			mImageView.setImageBitmap(bitmap);
			Log.e("jjjjj","kkk4");
		}
	}


	private boolean downloadUrlToStream(String url, OutputStream outputStream) {
		URLConnection urlcon=null;
		BufferedOutputStream out=null;
		BufferedInputStream in=null;
		if(url!=null){
			Log.e("sssssss",url);
			try {
				URL url1=new URL(url);
				urlcon=(URLConnection)url1.openConnection();
				in =new BufferedInputStream(urlcon.getInputStream(),8*1024);
				out=new BufferedOutputStream(outputStream,8*1024);
				int b;
				while((b=in.read())!=-1){
					out.write(b);
				}
				return true;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}finally {
				try {
					if(out!=null) {
						out.close();
					}
					if(in!=null){
						in.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	/**
	 *对图片进行简单的压缩
	 */
	private Bitmap getBitmap(FileDescriptor fileDescriptor){
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFileDescriptor(fileDescriptor,null,options);
		int imaHeight = options.outHeight;
		int imaWidth = options.outWidth;
		int viewHeight;
		int viewWidth;
		if(mHeight>0 && mWidth>0){
			viewHeight = mHeight;
			viewWidth = mWidth;
		}else{
			viewHeight = mImageView.getHeight();
			viewWidth = mImageView.getWidth();
		}
		int inSampleSize = 1;
		if (imaHeight > viewHeight || imaWidth > viewWidth) {
			// 计算出实际宽高和目标宽高的比率
			final int heightRatio = Math.round((float) imaHeight / (float) viewHeight);
			final int widthRatio = Math.round((float) imaWidth / (float) viewWidth);
			// 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
			// 一定都会大于等于目标的宽和高。
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}
		Log.e("blll",imaHeight+" "+viewHeight+ " "+imaWidth+" "+viewWidth+" "+inSampleSize);
		options.inSampleSize = inSampleSize;
		options.inJustDecodeBounds = false;

		return BitmapFactory.decodeFileDescriptor(fileDescriptor,null,options);
	}

	/**
	 *对图片进行简单的压缩
	 */
	private Bitmap getBitmap(int failImage){
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(mKlose.getContext().getResources(),failImage,options);
		int imaHeight = options.outHeight;
		int imaWidth = options.outWidth;
		int viewHeight;
		int viewWidth;
		if(mHeight>0 && mWidth>0){
			viewHeight = mHeight;
			viewWidth = mWidth;
		}else{
			viewHeight = mImageView.getHeight();
			viewWidth = mImageView.getWidth();
		}
		int inSampleSize = 1;
		if (imaHeight > viewHeight || imaWidth > viewWidth) {
			// 计算出实际宽高和目标宽高的比率
			final int heightRatio = Math.round((float) imaHeight / (float) viewHeight);
			final int widthRatio = Math.round((float) imaWidth / (float) viewWidth);
			// 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
			// 一定都会大于等于目标的宽和高。
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}
		Log.e("blll",imaHeight+" "+viewHeight+ " "+imaWidth+" "+viewWidth+" "+inSampleSize);
		options.inSampleSize = inSampleSize;
		options.inJustDecodeBounds = false;

		return BitmapFactory.decodeResource(mKlose.getContext().getResources(),failImage,options);
	}
}
