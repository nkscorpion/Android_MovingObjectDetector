package com.example.movingobjectdetector;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.WindowManager;

public class MainActivity extends Activity implements CvCameraViewListener2 {
	public static final String TAG = "OBJDETECT";
	
	private CameraBridgeViewBase mOpenCvCameraView;
	Mat frame, fgMaskMOG, mRgba, rgb, fgMskMOG_blur, obj_bin, thresh_output; 
	Mat mRgba_temp, morph1, morph2;
	Mat drawing;
	List<MatOfPoint> contours;
	List<MatOfPoint> contours_poly;
	MatOfRect boundRect;
	MatOfPoint2f mMOP2_1, mMOP2_2;
	MatOfPoint mMOP;
	MatOfInt4 hierarchy;
	Rect [] boundingRect;
	Scalar color;
	long len;
	
	Size ksize, morph_ksize;
	VideoCapture capture;
	int thresh = 100;
	int max_thresh = 255;
	BackgroundSubtractorMOG backgroundMOG;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
		
		
		
		
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this){
		
		public void onManagerConnected(int status){
			switch (status){
				case LoaderCallbackInterface.SUCCESS:
				{
					Log.i(TAG, "OpenCV loaded sucessfuly");
					
					//initialize Mats
					mRgba = new Mat();
					mRgba_temp = new Mat();
					fgMaskMOG = new Mat();
					rgb = new Mat();
					fgMskMOG_blur = new Mat();
					obj_bin = new Mat();
					thresh_output = new Mat();
					drawing = new Mat();
					morph1 = new Mat();
					morph2 = new Mat();
					
					hierarchy = new MatOfInt4();
					mMOP2_1 = new MatOfPoint2f();
					mMOP2_2 = new MatOfPoint2f();
					mMOP = new MatOfPoint();
					
					//init kernel size
					ksize = new Size();
					
					
					morph_ksize = new Size();
					
					//create new contours for findCountours function
					//contours = new ArrayList<MatOfPoint>();
					
					//create backgroundMOG
					backgroundMOG = new BackgroundSubtractorMOG();
					//enable camera
					mOpenCvCameraView.enableView();
			
					
				}break;
				
				default:
				{
					super.onManagerConnected(status);
				}break;	
			}		
		}		
	};


	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, mLoaderCallback);
	}

	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if(mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}
	
	

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}



	@Override
	public void onCameraViewStarted(int width, int height) {
		// TODO Auto-generated method stub
		Log.i(TAG, "in onCameraViewStarte");
	}


	@Override
	public void onCameraViewStopped() {
		// TODO Auto-generated method stub
		Log.i(TAG, "in onCameraViewStopped");
	}


	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		// TODO Auto-generated method stub
		Log.i(TAG, "in onCameraFrame");
		mRgba = inputFrame.rgba();
		mRgba.copyTo(mRgba_temp);
		
		Imgproc.cvtColor(mRgba, rgb, Imgproc.COLOR_RGBA2RGB);
		backgroundMOG.apply(rgb, fgMaskMOG, .1);
		
		
		ksize.height = ksize.width = 5.0;
		//blur edges to eliminate noise
		Imgproc.GaussianBlur(fgMaskMOG, fgMskMOG_blur, ksize, 2, 2);
		
		morph_ksize.height = morph_ksize.width = 3.0;
		Imgproc.erode(fgMaskMOG, morph1, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, morph_ksize, new Point(-1,-1)), new Point(-1, -1), 2);
		morph_ksize.height = morph_ksize.width = 5.0;
		Imgproc.dilate(morph1, morph1, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, morph_ksize, new Point(-1,-1)), new Point(-1, -1), 4);
//		Imgproc.morphologyEx(fgMskMOG_blur, morph1, Imgproc.MORPH_CLOSE, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, morph_ksize, new Point(3,3)));
//		Imgproc.morphologyEx(morph1, morph2, Imgproc.MORPH_CLOSE, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, morph_ksize, new Point(3,3)));
//		Imgproc.adaptiveThreshold(morph1, obj_bin, max_thresh,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C ,Imgproc.THRESH_BINARY, 3, 1);
		//get binary image using threshold
		Imgproc.threshold(morph1, obj_bin, thresh, max_thresh, Imgproc.THRESH_BINARY);
//		Core.inRange(morph2, new Scalar(100,100,100), new Scalar(120, 255, 255) , obj_bin);
		
		
		//contours is a list of a vector of points
		//create new contours for findCountours function
		contours = new ArrayList<MatOfPoint>();
	
		Imgproc.findContours(obj_bin, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0,0));
		  /// Approximate contours to polygons + get bounding rects and circles
		 // vector<vector<Point> > contours_poly( contours.size() );
		 // vector<Rect> boundRect( contours.size() );
		 // vector<Point2f>center( contours.size() );
		 // vector<float>radius( contours.size() )
		
		
		/****  ADD THESE 2 lines for complete sorta working implementation
		color = new Scalar(0, 0, 255);
		
		Imgproc.drawContours(mRgba_temp, contours, -1, color ,8);
		
		****/
			
		
		
		
		
		
		if(contours.size() != 0){
			boundingRect = new Rect[contours.size()];
//			contours_poly = new ArrayList<MatOfPoint>(contours.size());
			
			len = contours.size();
			for(int i = 0 ; i < len ; i++ ){
				contours.get(i).convertTo(mMOP2_1, CvType.CV_32FC2);
				Imgproc.approxPolyDP(mMOP2_1, mMOP2_2 , 3, true);
				mMOP2_2.convertTo(contours.get(i), CvType.CV_32S);
				boundingRect[i] = Imgproc.boundingRect(contours.get(i));
				
	
			//	mMOP2_2.convertTo(mMOP, CvType.CV_32S);
			//	mMOP.copyTo(contours_poly.get(i));
		    //  boundingRect[i] = Imgproc.boundingRect(mMOP);

				
			
			}
		
		
		
			color = new Scalar(0, 255, 0);
//			drawing = Mat.zeros(obj_bin.size(), CvType.CV_8UC3);
			for(int i = 0; i < contours.size(); i++){
			
	//			Imgproc.drawContours(mRgba, contours, i , color, 2);
				Core.rectangle(mRgba, boundingRect[i].tl(), boundingRect[i].br(), color, 2, 30, 0);
				
			}
		}
	

				
		return  mRgba;  
		
	}
	
	
	
	
	
	
}
