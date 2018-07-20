package com.mintwireless.mintegrate.console;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

public class CaptureSignature extends Activity {

	    LinearLayout mContent;
	    signature mSignature;
	    Button mClear,btn_done;
		TextView tv_date_time,tv_amount,tv_ref;
	    public static String tempDir;
	    public int count = 1;
	    public String current = null;
	    private Bitmap mBitmap;
	    View mView;
	    File mypath;
	    SignatureReceiver signatureReceiver;
	    private String uniqueId;
		Intent  intent;

	    @Override
	    public void onCreate(Bundle savedInstanceState)
	    {
	        super.onCreate(savedInstanceState);
			this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
	        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
			try{
	        setContentView(R.layout.activity_capture_signature);

				/*if (getResources().getBoolean(R.bool.landscape)) {
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				}
*/
	        tempDir = Environment.getExternalStorageDirectory() + "/GetSignature" + "/";
	        ContextWrapper cw = new ContextWrapper(getApplicationContext());
	        File directory = cw.getDir("GetSignature", Context.MODE_PRIVATE);

	        prepareDirectory();
	        uniqueId = getTodaysDate() + "_" + getCurrentTime() + "_" + Math.random();
	        current = uniqueId + ".png";
	        mypath= new File(directory,current);

				signatureReceiver = new SignatureReceiver();

			registerReceiver(signatureReceiver,new IntentFilter("close signature class"));

	        mContent = (LinearLayout) findViewById(R.id.linearLayout);
	        mSignature = new signature(this, null);
	        mSignature.setBackgroundColor(Color.WHITE);
	        mContent.addView(mSignature, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			intent = getIntent();
	       	initUI();
			}catch (Exception e){
				e.printStackTrace();
			}
	    }

		public void initUI() throws Exception{
			tv_date_time = (TextView) findViewById(R.id.tv_date);
			tv_date_time.setText(intent.getStringExtra("Date"));

			tv_amount = (TextView) findViewById(R.id.tv_amount);
			tv_amount.setText("Amount: $"+intent.getStringExtra("Amount"));

			tv_ref = (TextView) findViewById(R.id.tv_ref);
			tv_ref.setText("Ref: "+intent.getStringExtra("Ref"));

			mClear = (Button)findViewById(R.id.btn_clear);
			btn_done = (Button)findViewById(R.id.btn_done);
			btn_done.setEnabled(false);
			mView = mContent;


			mClear.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					Log.v("log_tag", "Panel Cleared");
					mSignature.clear();
					btn_done.setEnabled(false);
				}
			});

			btn_done.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					Log.v("log_tag", "Panel Saved");

						mView.setDrawingCacheEnabled(true);
						String temp = mSignature.save(mView);
						Intent intent = new Intent();
						intent.putExtra("status", "done");
						intent.putExtra("base", temp);
						setResult(RESULT_OK,intent);
						finish();

				}
			});

		}

	    @Override
	    protected void onDestroy() {
	        Log.w("GetSignature", "onDestory");
	        super.onDestroy();
			try {
				unregisterReceiver(signatureReceiver);
			}catch(Exception e){
				e.printStackTrace();
			}

	    }


	    private String getTodaysDate() {

	        final Calendar c = Calendar.getInstance();
	        int todaysDate =     (c.get(Calendar.YEAR) * 10000) +
	        ((c.get(Calendar.MONTH) + 1) * 100) +
	        (c.get(Calendar.DAY_OF_MONTH));
	        Log.w("DATE:", String.valueOf(todaysDate));
	        return(String.valueOf(todaysDate));

	    }

	    private String getCurrentTime() {

	        final Calendar c = Calendar.getInstance();
	        int currentTime =     (c.get(Calendar.HOUR_OF_DAY) * 10000) +
	        (c.get(Calendar.MINUTE) * 100) +
	        (c.get(Calendar.SECOND));
	        Log.w("TIME:", String.valueOf(currentTime));
	        return(String.valueOf(currentTime));

	    }

	    private boolean prepareDirectory()
	    {
	        try
	        {
	                return makedirs();
	        } catch (Exception e)
	        {
	            e.printStackTrace();
	            Toast.makeText(this, "Could not initiate File System.. Is Sdcard mounted properly?", Toast.LENGTH_SHORT).show();
	            return false;
	        }
	    }

	    private boolean makedirs()
	    {
	        File tempdir = new File(tempDir);

	        if (!tempdir.exists())
	            tempdir.mkdirs();

	        if (tempdir.isDirectory())
	        {
	            File[] files = tempdir.listFiles();
	            for (File file : files)
	            {
	                if (!file.delete())
	                {
	                    System.out.println("Failed to delete " + file);
	                }
	            }
	        }
	        return (tempdir.isDirectory());
	    }

	    public class signature extends View
	    {
	        private static final float STROKE_WIDTH = 5f;
	        private static final float HALF_STROKE_WIDTH = STROKE_WIDTH / 2;
	        private Paint paint = new Paint();
	        private Path path = new Path();

	        private float lastTouchX;
	        private float lastTouchY;
	        private final RectF dirtyRect = new RectF();

	        public signature(Context context, AttributeSet attrs)
	        {
	            super(context, attrs);
	            paint.setAntiAlias(true);
	            paint.setColor(Color.BLACK);
	            paint.setStyle(Paint.Style.STROKE);
	            paint.setStrokeJoin(Paint.Join.ROUND);
	            paint.setStrokeWidth(STROKE_WIDTH);
	        }

	        public byte[] getByteArray(){
	        	return null;
			}

	        public String save(View v)
	        {
				String temp ="";
	            Log.v("log_tag", "Width: " + v.getWidth());
	            Log.v("log_tag", "Height: " + v.getHeight());
	            if(mBitmap == null)
	            {
	                mBitmap =  Bitmap.createBitmap (mContent.getWidth(), mContent.getHeight(), Bitmap.Config.RGB_565);;
					ByteArrayOutputStream baos=new ByteArrayOutputStream();
					mBitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
					byte [] b=baos.toByteArray();
					temp=Base64.encodeToString(b, Base64.DEFAULT);

	            }
	            Canvas canvas = new Canvas(mBitmap);
	            try
	            {

					String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/appos";

					File dir = new File(path);
					if(!dir.exists())
						dir.mkdirs();

					File file = new File(dir, "signature.png");
	                FileOutputStream mFileOutStream = new FileOutputStream(file);

	                v.draw(canvas);
	                mBitmap.compress(Bitmap.CompressFormat.PNG, 90, mFileOutStream);
	                mFileOutStream.flush();
	                mFileOutStream.close();
//	                String url = Images.Media.insertImage(getContentResolver(), mBitmap, "title", null);
//	                Log.v("log_tag","url: " + url);
	                //In case you want to delete the file
	                //boolean deleted = mypath.delete();
	                //Log.v("log_tag","deleted: " + mypath.toString() + deleted);
	                //If you want to convert the image to string use base64 converter
					mBitmap.recycle();
	            }
	            catch(Exception e)
	            {
	                Log.v("log_tag", e.toString());
	            }
	            return temp;
	        }

	        public void clear()
	        {
	            path.reset();
	            invalidate();
	        }

	        @Override
	        protected void onDraw(Canvas canvas)
	        {
	            canvas.drawPath(path, paint);
	        }

	        @Override
	        public boolean onTouchEvent(MotionEvent event)
	        {
	            float eventX = event.getX();
	            float eventY = event.getY();
				btn_done.setEnabled(true);

	            switch (event.getAction())
	            {
	            case MotionEvent.ACTION_DOWN:
	                path.moveTo(eventX, eventY);
	                lastTouchX = eventX;
	                lastTouchY = eventY;
	                return true;

	            case MotionEvent.ACTION_MOVE:

	            case MotionEvent.ACTION_UP:

	                resetDirtyRect(eventX, eventY);
	                int historySize = event.getHistorySize();
	                for (int i = 0; i < historySize; i++)
	                {
	                    float historicalX = event.getHistoricalX(i);
	                    float historicalY = event.getHistoricalY(i);
	                    expandDirtyRect(historicalX, historicalY);
	                    path.lineTo(historicalX, historicalY);
	                }
	                path.lineTo(eventX, eventY);
	                break;

	            default:
	                debug("Ignored touch event: " + event.toString());
	                return false;
	            }

	            invalidate((int) (dirtyRect.left - HALF_STROKE_WIDTH),
	                    (int) (dirtyRect.top - HALF_STROKE_WIDTH),
	                    (int) (dirtyRect.right + HALF_STROKE_WIDTH),
	                    (int) (dirtyRect.bottom + HALF_STROKE_WIDTH));

	            lastTouchX = eventX;
	            lastTouchY = eventY;

	            return true;
	        }

	        private void debug(String string){
	        }

	        private void expandDirtyRect(float historicalX, float historicalY)
	        {
	            if (historicalX < dirtyRect.left)
	            {
	                dirtyRect.left = historicalX;
	            }
	            else if (historicalX > dirtyRect.right)
	            {
	                dirtyRect.right = historicalX;
	            }

	            if (historicalY < dirtyRect.top)
	            {
	                dirtyRect.top = historicalY;
	            }
	            else if (historicalY > dirtyRect.bottom)
	            {
	                dirtyRect.bottom = historicalY;
	            }
	        }

	        private void resetDirtyRect(float eventX, float eventY)
	        {
	            dirtyRect.left = Math.min(lastTouchX, eventX);
	            dirtyRect.right = Math.max(lastTouchX, eventX);
	            dirtyRect.top = Math.min(lastTouchY, eventY);
	            dirtyRect.bottom = Math.max(lastTouchY, eventY);
	        }
	    }

		public class SignatureReceiver extends BroadcastReceiver
		{

			@Override
			public void onReceive(Context context, Intent intent) {

				try {
					unregisterReceiver(signatureReceiver);
				}catch(Exception e){
					e.printStackTrace();
				}
					String resultText = intent.getStringExtra("ResultText");
					if(resultText != null){
						if(resultText.equalsIgnoreCase("Declined")){
							CaptureSignature.this.setResult(RESULT_OK, intent);

						}else{
							mView.setDrawingCacheEnabled(true);
							mSignature.save(mView);
							CaptureSignature.this.setResult(RESULT_OK, intent);
						}


						finish();
					}





			}
		}



	}